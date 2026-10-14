package com.ruoyiliteflow.liteflow.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.liteflow.constant.LiteFlowAuditAction;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.mapper.LfChainVersionMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.spring.SpringAware;

import static org.junit.Assert.*;

/** 使用真实 LiteFlow 验证失败发布与原有执行隔离，持久化仅保存在内存。 */
public class LfChainPublishConsistencyTest
{
    private static final AtomicLong IDS = new AtomicLong();
    private LfChainServiceImpl service;
    private FlowExecutor executor;
    private LiteflowConfig config;
    private GenericApplicationContext context;
    private final Map<Long, LfChain> rows = new LinkedHashMap<>();
    private final List<LfChainVersion> versions = new ArrayList<>();
    private final List<Audit> audits = new ArrayList<>();
    private final Set<String> scriptIds = new LinkedHashSet<>();
    private final Set<String> chainIds = new LinkedHashSet<>();
    private String oldNode;
    private String newNode;
    private int insertCalls;
    private int updateCalls;

    @Before
    public void setUp() throws Exception
    {
        // 独立 XML 加载避免 SQL 插件读取真实数据源，也不启动 Web 或应用自动配置。
        context = new GenericApplicationContext();
        context.refresh();
        new SpringAware().setApplicationContext(context);
        config = new LiteflowConfig();
        config.setRuleSource("classpath:runtime-regression.xml");
        executor = new FlowExecutor(config);
        oldNode = registerScript("旧规则结果");
        newNode = registerScript("新规则结果");
        service = new LfChainServiceImpl();
        LfChainMapper mapper = (LfChainMapper) Proxy.newProxyInstance(
            LfChainMapper.class.getClassLoader(), new Class<?>[] {LfChainMapper.class},
            (proxy, method, args) -> {
                switch (method.getName())
                {
                    case "selectLfChainById":
                        return copy(rows.get((Long) args[0]));
                    case "selectLfChainByName":
                        for (LfChain row : rows.values())
                        {
                            if (row.getChainName().equals(args[0])) { return copy(row); }
                        }
                        return null;
                    case "insertLfChain":
                        insertCalls++;
                        LfChain added = (LfChain) args[0];
                        added.setId(IDS.incrementAndGet());
                        rows.put(added.getId(), copy(added));
                        return 1;
                    case "updateLfChain":
                        updateCalls++;
                        LfChain changed = (LfChain) args[0];
                        rows.put(changed.getId(), copy(changed));
                        return 1;
                    case "selectLfChainList":
                        return new ArrayList<>(rows.values());
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
        LfChainVersionMapper versionMapper = (LfChainVersionMapper) Proxy.newProxyInstance(
            LfChainVersionMapper.class.getClassLoader(), new Class<?>[] {LfChainVersionMapper.class},
            (proxy, method, args) -> {
                if ("insertLfChainVersion".equals(method.getName()))
                {
                    versions.add((LfChainVersion) args[0]);
                    return 1;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        ILfChainAuditService auditService = (ILfChainAuditService) Proxy.newProxyInstance(
            ILfChainAuditService.class.getClassLoader(), new Class<?>[] {ILfChainAuditService.class},
            (proxy, method, args) -> {
                if ("recordChange".equals(method.getName()))
                {
                    audits.add(new Audit(copy((LfChain) args[0]), copy((LfChain) args[1]),
                        (String) args[2], (String) args[3]));
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
        inject("lfChainMapper", mapper);
        inject("lfChainVersionMapper", versionMapper);
        inject("lfChainAuditService", auditService);
        inject("lfChainPermissionService", permissionService);
    }

    @After
    public void tearDown()
    {
        for (String chainId : chainIds) { LiteflowMetaOperator.removeChain(chainId); }
        for (String scriptId : scriptIds) { FlowBus.unloadScriptNode(scriptId); }
        LiteflowMetaOperator.removeChain("regressionFixtureBootstrapChain");
        FlowBus.unloadScriptNode("regressionFixtureBootstrap");
        if (context != null) { context.close(); }
    }

    @Test
    public void missingNodeFirstPublishLeavesDraftVersionAuditAndSnapshotUnchanged()
    {
        LfChain draft = storedDraft("THEN(nonexistentPublishFixtureNode);");
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertFailedPublishNotPersisted(draft);
        assertNull(FlowBus.getChain(draft.getChainName()));
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    @Test
    public void invalidElFirstPublishLeavesNoRuntimeOrPublishedRecord()
    {
        LfChain draft = storedDraft("THEN(");
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertFailedPublishNotPersisted(draft);
        assertNull(FlowBus.getChain(draft.getChainName()));
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    @Test
    public void firstExecutionParseModeStillRejectsMissingNodeDuringPublish()
    {
        config.setParseMode(ParseModeEnum.PARSE_ONE_ON_FIRST_EXEC);
        LfChain draft = storedDraft("THEN(nonexistentLazyPublishFixtureNode);");
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertFailedPublishNotPersisted(draft);
        assertNull(FlowBus.getChain(draft.getChainName()));
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    @Test
    public void failedRepublishKeepsOldExecutionAndOtherChains()
    {
        LfChain draft = storedDraft("THEN(nonexistentPublishFixtureNode);");
        LiteflowMetaOperator.reloadOneChain(draft.getChainName(), el(oldNode));
        LfChain independent = storedDraft(el(newNode));
        LiteflowMetaOperator.reloadOneChain(independent.getChainName(), independent.getElData());
        assertEquals("旧规则结果", execute(draft.getChainName()));
        assertEquals("新规则结果", execute(independent.getChainName()));
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertFailedPublishNotPersisted(draft);
        assertEquals("旧规则结果", execute(draft.getChainName()));
        assertEquals("新规则结果", execute(independent.getChainName()));
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    @Test
    public void failedRouteCompileKeepsOldExecutionAndDraft()
    {
        LfChain draft = storedDraft(el(newNode));
        draft.setNamespace("合成测试路由空间");
        draft.setRouteEl("AND(");
        rows.put(draft.getId(), copy(draft));
        LiteflowMetaOperator.reloadOneChain(draft.getChainName(), el(oldNode));
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        assertThrows(ServiceException.class, () -> service.publishChain(draft.getId(), "合成测试发布者"));
        assertFailedPublishNotPersisted(draft);
        assertEquals("旧规则结果", execute(draft.getChainName()));
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    @Test
    public void successfulPublishProducesMatchingRuntimeVersionAuditAndSnapshot()
    {
        LfChain draft = storedDraft(el(newNode));
        LfChain independent = storedDraft(el(oldNode));
        LiteflowMetaOperator.reloadOneChain(independent.getChainName(), independent.getElData());
        Set<String> runtimeBefore = new HashSet<>(FlowBus.getChainMap().keySet());
        service.publishChain(draft.getId(), "合成测试发布者");
        LfChain saved = rows.get(draft.getId());
        assertEquals("0", saved.getDraftFlag());
        assertEquals(4, saved.getVersion().intValue());
        assertEquals("合成测试发布者", saved.getUpdateBy());
        assertEquals("新规则结果", execute(draft.getChainName()));
        assertEquals("旧规则结果", execute(independent.getChainName()));
        assertEquals(1, updateCalls);
        assertEquals(1, audits.size());
        Audit audit = audits.get(0);
        assertEquals(LiteFlowAuditAction.PUBLISH, audit.action);
        assertEquals("合成测试发布者", audit.operator);
        assertEquals("1", audit.before.getDraftFlag());
        assertEquals(3, audit.before.getVersion().intValue());
        assertEquals(saved.getVersion(), audit.after.getVersion());
        assertEquals(saved.getElData(), audit.after.getElData());
        assertEquals(1, versions.size());
        LfChainVersion version = versions.get(0);
        assertEquals(saved.getId(), version.getChainId());
        assertEquals(saved.getChainName(), version.getChainName());
        assertEquals(saved.getVersion(), version.getVersion());
        assertEquals(saved.getElData(), version.getElData());
        assertEquals(saved.getGraphJson(), version.getGraphJson());
        assertEquals("合成测试发布者", version.getPublishBy());
        runtimeBefore.add(draft.getChainName());
        assertEquals(runtimeBefore, new HashSet<>(FlowBus.getChainMap().keySet()));
    }

    private void assertFailedPublishNotPersisted(LfChain draft)
    {
        LfChain saved = rows.get(draft.getId());
        assertEquals("1", saved.getDraftFlag());
        assertEquals(draft.getVersion(), saved.getVersion());
        assertEquals(draft.getUpdateBy(), saved.getUpdateBy());
        assertEquals(draft.getElData(), saved.getElData());
        assertEquals(0, updateCalls);
        assertTrue(audits.isEmpty());
        assertTrue(versions.isEmpty());
    }

    private LfChain storedDraft(String expression)
    {
        LfChain chain = new LfChain();
        chain.setId(IDS.incrementAndGet());
        chain.setChainName("publishFixtureChain" + IDS.incrementAndGet());
        chain.setApplicationName("ruoyi-liteflow");
        chain.setChainDesc("合成发布一致性测试");
        chain.setElData(expression);
        chain.setGraphJson("{\"fixture\":true}");
        chain.setEnable(1);
        chain.setStatus("0");
        chain.setDraftFlag("1");
        chain.setVersion(3);
        chain.setUpdateBy("合成草稿编辑者");
        rows.put(chain.getId(), copy(chain));
        chainIds.add(chain.getChainName());
        return chain;
    }

    private String registerScript(String value)
    {
        String scriptId = "publishFixtureScript" + IDS.incrementAndGet();
        scriptIds.add(scriptId);
        FlowBus.addScriptNodeAndCompile(scriptId, "合成发布测试节点", NodeTypeEnum.SCRIPT,
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

    private static String el(String scriptId)
    {
        return "THEN(" + scriptId + ");";
    }

    private void inject(String fieldName, Object value) throws Exception
    {
        Field field = LfChainServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static LfChain copy(LfChain row)
    {
        if (row == null) { return null; }
        // Mapper 返回副本，避免待发布对象在写库前改变测试中的已保存状态。
        LfChain copy = new LfChain();
        copy.setId(row.getId());
        copy.setApplicationName(row.getApplicationName());
        copy.setChainName(row.getChainName());
        copy.setChainDesc(row.getChainDesc());
        copy.setElData(row.getElData());
        copy.setGraphJson(row.getGraphJson());
        copy.setEnable(row.getEnable());
        copy.setStatus(row.getStatus());
        copy.setDraftFlag(row.getDraftFlag());
        copy.setVersion(row.getVersion());
        copy.setContextClass(row.getContextClass());
        copy.setRouteEl(row.getRouteEl());
        copy.setNamespace(row.getNamespace());
        copy.setWebhookUrl(row.getWebhookUrl());
        copy.setCreateBy(row.getCreateBy());
        copy.setUpdateBy(row.getUpdateBy());
        copy.setRemark(row.getRemark());
        return copy;
    }

    private static class Audit
    {
        private final LfChain before;
        private final LfChain after;
        private final String action;
        private final String operator;

        private Audit(LfChain before, LfChain after, String action, String operator)
        {
            this.before = before;
            this.after = after;
            this.action = action;
            this.operator = operator;
        }
    }
}
