package com.ruoyiliteflow.liteflow.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionStatus;
import cn.hutool.crypto.digest.MD5;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.mapper.LfChainVersionMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.spring.SpringAware;
import com.yomahub.liteflow.util.ElRegexUtil;

import static org.junit.Assert.*;

/** 使用 H2 内存库和真实 Spring 事务代理验证整笔发布回滚，不连接外部数据库。 */
public class LfChainPublishTransactionTest
{
    private static final AtomicLong IDS = new AtomicLong();
    private GenericApplicationContext context;
    private FlowExecutor executor;
    private ILfChainService service;
    private JdbcTemplate jdbc;
    private FaultTransactionManager transactionManager;
    private String failurePoint;
    private String oldNode;
    private String newNode;
    private final Set<String> scriptIds = new LinkedHashSet<>();
    private final Set<String> chainIds = new LinkedHashSet<>();

    @Before
    public void setUp() throws Exception
    {
        context = new GenericApplicationContext();
        context.refresh();
        new SpringAware().setApplicationContext(context);
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("classpath:runtime-regression.xml");
        executor = new FlowExecutor(config);
        oldNode = registerScript("旧事务规则结果");
        newNode = registerScript("新事务规则结果");

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:publish_fixture_" + IDS.incrementAndGet() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        // 这些最小表只服务于事务回归，不代表平台业务库的表结构设计。
        jdbc.execute("CREATE TABLE chain_fixture (id BIGINT PRIMARY KEY, chain_name VARCHAR(128), "
            + "el_data VARCHAR(4000), graph_json VARCHAR(4000), draft_flag VARCHAR(1), revision INT, "
            + "update_by VARCHAR(128), enabled INT, chain_status VARCHAR(1))");
        jdbc.execute("CREATE TABLE audit_fixture (chain_id BIGINT, action_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE version_fixture (chain_id BIGINT, revision INT, el_data VARCHAR(4000))");

        LfChainServiceImpl target = new LfChainServiceImpl();
        LfChainMapper mapper = (LfChainMapper) Proxy.newProxyInstance(
            LfChainMapper.class.getClassLoader(), new Class<?>[] {LfChainMapper.class},
            (proxy, method, args) -> {
                switch (method.getName())
                {
                    case "selectLfChainById":
                        return select("id", args[0]);
                    case "selectLfChainByName":
                        return select("chain_name", args[0]);
                    case "insertLfChain":
                        LfChain added = (LfChain) args[0];
                        if (added.getId() == null) { added.setId(IDS.incrementAndGet()); }
                        insert(added);
                        failAfterWrite("insert");
                        return 1;
                    case "updateLfChain":
                        LfChain changed = (LfChain) args[0];
                        int updated = jdbc.update("UPDATE chain_fixture SET chain_name=?, el_data=?, "
                            + "graph_json=?, draft_flag=?, revision=?, update_by=?, enabled=?, chain_status=? WHERE id=?",
                            changed.getChainName(), changed.getElData(), changed.getGraphJson(), changed.getDraftFlag(),
                            changed.getVersion(), changed.getUpdateBy(), changed.getEnable(), changed.getStatus(), changed.getId());
                        failAfterWrite("update");
                        return updated;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
        LfChainVersionMapper versionMapper = (LfChainVersionMapper) Proxy.newProxyInstance(
            LfChainVersionMapper.class.getClassLoader(), new Class<?>[] {LfChainVersionMapper.class},
            (proxy, method, args) -> {
                if ("insertLfChainVersion".equals(method.getName()))
                {
                    LfChainVersion version = (LfChainVersion) args[0];
                    jdbc.update("INSERT INTO version_fixture(chain_id, revision, el_data) VALUES(?,?,?)",
                        version.getChainId(), version.getVersion(), version.getElData());
                    failAfterWrite("snapshot");
                    return 1;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        ILfChainAuditService auditService = (ILfChainAuditService) Proxy.newProxyInstance(
            ILfChainAuditService.class.getClassLoader(), new Class<?>[] {ILfChainAuditService.class},
            (proxy, method, args) -> {
                if ("recordChange".equals(method.getName()))
                {
                    LfChain after = (LfChain) args[1];
                    jdbc.update("INSERT INTO audit_fixture(chain_id, action_type) VALUES(?,?)", after.getId(), args[2]);
                    failAfterWrite("audit");
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        ILfChainPermissionService permissionService = (ILfChainPermissionService) Proxy.newProxyInstance(
            ILfChainPermissionService.class.getClassLoader(), new Class<?>[] {ILfChainPermissionService.class},
            (proxy, method, args) -> {
                if ("assertCanEdit".equals(method.getName())) { return null; }
                throw new UnsupportedOperationException(method.getName());
            });
        inject(target, "lfChainMapper", mapper);
        inject(target, "lfChainVersionMapper", versionMapper);
        inject(target, "lfChainAuditService", auditService);
        inject(target, "lfChainPermissionService", permissionService);
        transactionManager = new FaultTransactionManager(dataSource);
        transactionManager.setRollbackOnCommitFailure(true);
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(ILfChainService.class);
        factory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        service = (ILfChainService) factory.getProxy();
    }

    @After
    public void tearDown()
    {
        for (String chainId : chainIds) { LiteflowMetaOperator.removeChain(chainId); }
        for (String scriptId : scriptIds) { FlowBus.unloadScriptNode(scriptId); }
        LiteflowMetaOperator.removeChain("regressionFixtureBootstrapChain");
        FlowBus.unloadScriptNode("regressionFixtureBootstrap");
        if (jdbc != null) { jdbc.execute("SHUTDOWN"); }
        if (context != null) { context.close(); }
    }

    @Test
    public void auditWriteFailureRollsBackDatabaseAndRestoresOldRuntime()
    {
        assertExistingPublishRollback("audit");
    }

    @Test
    public void snapshotWriteFailureRollsBackDatabaseAuditAndOldRuntime()
    {
        assertExistingPublishRollback("snapshot");
    }

    @Test
    public void chainWriteFailureRollsBackDatabaseAndRestoresOldRuntime()
    {
        assertExistingPublishRollback("update");
    }

    @Test
    public void firstPublishSnapshotFailureRemovesNewRuntimeChain()
    {
        LfChain draft = newChain(el(newNode), "1");
        insert(draft);
        String candidateHash = elHash(draft.getElData());
        failurePoint = "snapshot";
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertDatabaseUnchanged(draft);
        assertNull(FlowBus.getChain(draft.getChainName()));
        assertNull(FlowBus.getChainIdByElMd5(candidateHash));
    }

    @Test
    public void publishedInsertCompileFailureRollsBackRowAndAudit()
    {
        LfChain added = newChain("THEN(nonexistentTransactionFixtureNode);", "0");
        assertThrows(ServiceException.class, () -> service.insertLfChain(added));
        assertNull(select("id", added.getId()));
        assertEquals(0, count("audit_fixture"));
        assertEquals(0, count("version_fixture"));
        assertNull(FlowBus.getChain(added.getChainName()));
    }

    @Test
    public void publishedEditCompileFailureRollsBackRowAndKeepsOldExecution()
    {
        LfChain original = newChain(el(oldNode), "0");
        insert(original);
        LiteflowMetaOperator.reloadOneChain(original.getChainName(), original.getElData());
        Chain previous = FlowBus.getChain(original.getChainName());
        String previousHash = previous.getElMd5();
        LfChain changed = select("id", original.getId());
        changed.setElData("THEN(nonexistentTransactionFixtureNode);");
        assertThrows(ServiceException.class, () -> service.updateLfChain(changed));
        assertDatabaseUnchanged(original);
        assertEquals("旧事务规则结果", execute(original.getChainName()));
        assertRestoredRuntime(previous, previousHash, elHash(changed.getElData()));
    }

    @Test
    public void simulatedFailureBeforeCommitRollsBackAndRestoresRuntime()
    {
        LfChain draft = existingRuntimeWithNewDraft();
        Chain previous = FlowBus.getChain(draft.getChainName());
        String previousHash = previous.getElMd5();
        String candidateHash = elHash(draft.getElData());
        transactionManager.failBeforeCommit = true;
        assertThrows(TransactionSystemException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertDatabaseUnchanged(draft);
        assertEquals("旧事务规则结果", execute(draft.getChainName()));
        assertRestoredRuntime(previous, previousHash, candidateHash);
    }

    @Test
    public void successfulCommitKeepsPublishedVersionAuditSnapshotAndParentUsesNewChild()
    {
        LfChain draft = existingRuntimeWithNewDraft();
        Chain previous = FlowBus.getChain(draft.getChainName());
        String parentId = parentFor(draft.getChainName());
        assertEquals("旧事务规则结果", execute(parentId));
        service.publishChain(draft.getId(), "合成测试发布者");
        LfChain saved = select("id", draft.getId());
        assertEquals("0", saved.getDraftFlag());
        assertEquals(4, saved.getVersion().intValue());
        assertEquals(1, count("audit_fixture"));
        assertEquals(1, count("version_fixture"));
        assertEquals(saved.getVersion(), jdbc.queryForObject("SELECT revision FROM version_fixture", Integer.class));
        assertEquals(saved.getElData(), jdbc.queryForObject("SELECT el_data FROM version_fixture", String.class));
        assertEquals("新事务规则结果", execute(draft.getChainName()));
        assertSame(previous, FlowBus.getChain(draft.getChainName()));
        // 不重建父链，验证发布保留子链对象身份，使已编译引用获得新的执行条件。
        assertEquals("新事务规则结果", execute(parentId));
    }

    private void assertExistingPublishRollback(String point)
    {
        LfChain draft = existingRuntimeWithNewDraft();
        Chain previous = FlowBus.getChain(draft.getChainName());
        String previousHash = previous.getElMd5();
        String candidateHash = elHash(draft.getElData());
        assertNotEquals(previousHash, candidateHash);
        String parentId = parentFor(draft.getChainName());
        assertEquals("旧事务规则结果", execute(parentId));
        failurePoint = point;
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertDatabaseUnchanged(draft);
        assertEquals("旧事务规则结果", execute(draft.getChainName()));
        assertEquals("旧事务规则结果", execute(parentId));
        assertRestoredRuntime(previous, previousHash, candidateHash);
    }

    private void assertRestoredRuntime(Chain previous, String previousHash, String candidateHash)
    {
        assertSame(previous, FlowBus.getChain(previous.getChainId()));
        assertEquals(previousHash, previous.getElMd5());
        assertEquals(previous.getChainId(), FlowBus.getChainIdByElMd5(previousHash));
        // 回滚后不能留下新表达式缓存，否则按该表达式执行时会误命中恢复的旧链。
        assertNull(FlowBus.getChainIdByElMd5(candidateHash));
    }

    private static String elHash(String expression)
    {
        return MD5.create().digestHex(ElRegexUtil.normalize(expression));
    }

    private LfChain existingRuntimeWithNewDraft()
    {
        LfChain draft = newChain(el(newNode), "1");
        insert(draft);
        LiteflowMetaOperator.reloadOneChain(draft.getChainName(), el(oldNode));
        return draft;
    }

    private void assertDatabaseUnchanged(LfChain original)
    {
        LfChain saved = select("id", original.getId());
        assertNotNull(saved);
        assertEquals(original.getDraftFlag(), saved.getDraftFlag());
        assertEquals(original.getVersion(), saved.getVersion());
        assertEquals(original.getElData(), saved.getElData());
        assertEquals(original.getGraphJson(), saved.getGraphJson());
        assertEquals(original.getUpdateBy(), saved.getUpdateBy());
        assertEquals(0, count("audit_fixture"));
        assertEquals(0, count("version_fixture"));
    }

    private String parentFor(String childId)
    {
        String parentId = "transactionFixtureParent" + IDS.incrementAndGet();
        chainIds.add(parentId);
        LiteflowMetaOperator.reloadOneChain(parentId, el(childId));
        return parentId;
    }

    private LfChain newChain(String expression, String draftFlag)
    {
        LfChain chain = new LfChain();
        chain.setId(IDS.incrementAndGet());
        chain.setChainName("transactionFixtureChain" + IDS.incrementAndGet());
        chain.setApplicationName("ruoyi-liteflow");
        chain.setChainDesc("合成事务一致性测试");
        chain.setElData(expression);
        chain.setGraphJson("{\"transactionFixture\":true}");
        chain.setDraftFlag(draftFlag);
        chain.setVersion(3);
        chain.setEnable(1);
        chain.setStatus("0");
        chain.setCreateBy("合成草稿创建者");
        chain.setUpdateBy("合成草稿编辑者");
        chainIds.add(chain.getChainName());
        return chain;
    }

    private void insert(LfChain row)
    {
        jdbc.update("INSERT INTO chain_fixture(id, chain_name, el_data, graph_json, draft_flag, revision, "
            + "update_by, enabled, chain_status) VALUES(?,?,?,?,?,?,?,?,?)",
            row.getId(), row.getChainName(), row.getElData(), row.getGraphJson(), row.getDraftFlag(), row.getVersion(),
            row.getUpdateBy(), row.getEnable(), row.getStatus());
    }

    private LfChain select(String column, Object value)
    {
        List<LfChain> found = jdbc.query("SELECT * FROM chain_fixture WHERE " + column + "=?", (rs, index) -> {
            LfChain row = new LfChain();
            row.setId(rs.getLong("id"));
            row.setChainName(rs.getString("chain_name"));
            row.setApplicationName("ruoyi-liteflow");
            row.setElData(rs.getString("el_data"));
            row.setGraphJson(rs.getString("graph_json"));
            row.setDraftFlag(rs.getString("draft_flag"));
            row.setVersion(rs.getInt("revision"));
            row.setUpdateBy(rs.getString("update_by"));
            row.setEnable(rs.getInt("enabled"));
            row.setStatus(rs.getString("chain_status"));
            return row;
        }, value);
        return found.isEmpty() ? null : found.get(0);
    }

    private int count(String table)
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void failAfterWrite(String point)
    {
        if (point.equals(failurePoint))
        {
            throw new ServiceException("合成事务测试：模拟" + point + "写入后失败");
        }
    }

    private String registerScript(String value)
    {
        String scriptId = "transactionFixtureScript" + IDS.incrementAndGet();
        scriptIds.add(scriptId);
        FlowBus.addScriptNodeAndCompile(scriptId, "合成事务测试节点", NodeTypeEnum.SCRIPT,
            "defaultContext.setData('result', '" + value + "')", "groovy");
        return scriptId;
    }

    private String execute(String chainId)
    {
        LiteflowResponse response = executor.execute2Resp(chainId, null, DefaultContext.class);
        assertTrue(response.getMessage(), response.isSuccess());
        DefaultContext context = response.getContextBean(DefaultContext.class);
        return context.getData("result");
    }

    private static String el(String nodeOrChainId)
    {
        return "THEN(" + nodeOrChainId + ");";
    }

    private void inject(Object target, String fieldName, Object value) throws Exception
    {
        Field field = LfChainServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FaultTransactionManager extends DataSourceTransactionManager
    {
        private boolean failBeforeCommit;

        private FaultTransactionManager(JdbcDataSource dataSource)
        {
            super(dataSource);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status)
        {
            if (failBeforeCommit)
            {
                failBeforeCommit = false;
                // 模拟物理提交之前的异常，不声称覆盖网络断开导致提交结果未知的场景。
                throw new TransactionSystemException("合成事务测试：模拟提交前失败");
            }
            super.doCommit(status);
        }
    }
}
