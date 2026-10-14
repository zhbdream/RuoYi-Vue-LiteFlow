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
import com.ruoyiliteflow.liteflow.domain.LfScript;
import com.ruoyiliteflow.liteflow.domain.LfScriptVersion;
import com.ruoyiliteflow.liteflow.mapper.LfScriptMapper;
import com.ruoyiliteflow.liteflow.mapper.LfScriptVersionMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.spring.SpringAware;

import static org.junit.Assert.*;

/** 使用真实规则运行时验证注册与热更新；Mapper 只在内存保存，不连接数据库。 */
public class LfScriptRuntimeRegistrationTest
{
    private static final AtomicLong IDS = new AtomicLong();
    private LfScriptServiceImpl service;
    private FlowExecutor executor;
    private GenericApplicationContext context;
    private final Map<Long, LfScript> rows = new LinkedHashMap<>();
    private final List<LfScriptVersion> versions = new ArrayList<>();
    private final Set<String> scriptIds = new LinkedHashSet<>();
    private final Set<String> chainIds = new LinkedHashSet<>();
    private int insertCalls;
    private int updateCalls;

    @Before
    public void setUp() throws Exception
    {
        // 使用无自动配置的空容器满足 LiteFlow 的 Spring SPI，不启动 Web、数据源或应用服务。
        context = new GenericApplicationContext();
        context.refresh();
        new SpringAware().setApplicationContext(context);
        LiteflowConfig config = new LiteflowConfig();
        // 显式使用合成 XML，避免 SQL 解析插件按默认 SPI 寻找数据库配置。
        config.setRuleSource("classpath:runtime-regression.xml");
        executor = new FlowExecutor(config);
        service = new LfScriptServiceImpl();
        LfScriptMapper mapper = (LfScriptMapper) Proxy.newProxyInstance(
            LfScriptMapper.class.getClassLoader(), new Class<?>[] {LfScriptMapper.class},
            (proxy, method, args) -> {
                switch (method.getName())
                {
                    case "selectLfScriptById":
                        return copy(rows.get((Long) args[0]));
                    case "selectLfScriptByScriptId":
                        for (LfScript row : rows.values())
                        {
                            if (row.getScriptId().equals(args[0])) { return copy(row); }
                        }
                        return null;
                    case "insertLfScript":
                        insertCalls++;
                        LfScript added = (LfScript) args[0];
                        added.setId(IDS.incrementAndGet());
                        rows.put(added.getId(), copy(added));
                        return 1;
                    case "updateLfScript":
                        updateCalls++;
                        LfScript changed = (LfScript) args[0];
                        rows.put(changed.getId(), copy(changed));
                        return 1;
                    case "selectLfScriptList":
                        return new ArrayList<>(rows.values());
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
        LfScriptVersionMapper versionMapper = (LfScriptVersionMapper) Proxy.newProxyInstance(
            LfScriptVersionMapper.class.getClassLoader(), new Class<?>[] {LfScriptVersionMapper.class},
            (proxy, method, args) -> {
                if ("insert".equals(method.getName()))
                {
                    versions.add((LfScriptVersion) args[0]);
                    return 1;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        inject("lfScriptMapper", mapper);
        inject("lfScriptVersionMapper", versionMapper);
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
    public void firstEnabledSaveRegistersNodeAndExecutesNewChain()
    {
        LfScript script = script("首个结果", 1);
        assertNull(FlowBus.getNode(script.getScriptId()));
        assertEquals(1, service.insertLfScript(script));
        assertNotNull(FlowBus.getNode(script.getScriptId()));
        assertEquals("首个结果", execute(script));
        assertEquals(1, insertCalls);
    }

    @Test
    public void editingExistingScriptHotUpdatesAlreadyBuiltChain()
    {
        LfScript script = script("旧结果", 1);
        service.insertLfScript(script);
        String chainId = buildChain(script);
        assertEquals("旧结果", executeChain(chainId));
        script.setScriptData(source("新结果"));
        service.updateLfScript(script);
        // 不重建规则链，确认修改作用于原来的运行时节点。
        assertEquals("新结果", executeChain(chainId));
        assertEquals(2, script.getVersion().intValue());
        assertEquals(1, versions.size());
        assertEquals(source("旧结果"), versions.get(0).getScriptData());
    }

    @Test
    public void firstEnableOfDisabledSavedScriptRegistersNode()
    {
        LfScript script = script("启用结果", 0);
        service.insertLfScript(script);
        assertNull(FlowBus.getNode(script.getScriptId()));
        script.setEnable(1);
        service.updateLfScript(script);
        assertNotNull(FlowBus.getNode(script.getScriptId()));
        assertEquals("启用结果", execute(script));
        assertTrue(versions.isEmpty());
    }

    @Test
    public void invalidNewScriptFailsBeforeMapperWriteAndLeavesNoNode()
    {
        LfScript script = script("不会执行", 1);
        script.setScriptData("def broken = (");
        Set<String> before = new HashSet<>(FlowBus.getNodeMap().keySet());
        assertThrows(ServiceException.class, () -> service.insertLfScript(script));
        assertEquals(0, insertCalls);
        assertTrue(rows.isEmpty());
        assertNull(FlowBus.getNode(script.getScriptId()));
        assertEquals(before, new HashSet<>(FlowBus.getNodeMap().keySet()));
    }

    @Test
    public void invalidEditPreservesSavedScriptAndExistingExecution()
    {
        LfScript script = script("保留旧结果", 1);
        service.insertLfScript(script);
        String chainId = buildChain(script);
        assertEquals("保留旧结果", executeChain(chainId));
        script.setScriptData("def broken = (");
        Set<String> before = new HashSet<>(FlowBus.getNodeMap().keySet());
        assertThrows(ServiceException.class, () -> service.updateLfScript(script));
        assertEquals(0, updateCalls);
        assertEquals(source("保留旧结果"), rows.get(script.getId()).getScriptData());
        assertEquals("保留旧结果", executeChain(chainId));
        assertTrue(versions.isEmpty());
        assertEquals(before, new HashSet<>(FlowBus.getNodeMap().keySet()));
    }

    @Test
    public void successfulSyntaxValidationDoesNotRegisterPermanentOrTemporaryNode()
    {
        LfScript script = script("只校验", 1);
        Set<String> before = new HashSet<>(FlowBus.getNodeMap().keySet());
        service.validateScript(script);
        assertNull(FlowBus.getNode(script.getScriptId()));
        assertEquals(before, new HashSet<>(FlowBus.getNodeMap().keySet()));
        assertEquals(0, insertCalls);
    }

    private LfScript script(String value, int enabled)
    {
        LfScript script = new LfScript();
        script.setScriptId("regressionScript" + IDS.incrementAndGet());
        script.setScriptName("合成注册回归");
        script.setScriptType("script");
        script.setScriptLanguage("groovy");
        script.setEnable(enabled);
        script.setScriptData(source(value));
        scriptIds.add(script.getScriptId());
        return script;
    }

    private static String source(String value)
    {
        return "defaultContext.setData('result', '" + value + "')";
    }

    private String buildChain(LfScript script)
    {
        String chainId = "regressionChain" + IDS.incrementAndGet();
        chainIds.add(chainId);
        LiteflowMetaOperator.reloadOneChain(chainId, "THEN(" + script.getScriptId() + ");");
        return chainId;
    }

    private String execute(LfScript script)
    {
        return executeChain(buildChain(script));
    }

    private String executeChain(String chainId)
    {
        LiteflowResponse response = executor.execute2Resp(chainId, null, DefaultContext.class);
        assertTrue(response.getMessage(), response.isSuccess());
        DefaultContext context = response.getContextBean(DefaultContext.class);
        return context.getData("result");
    }

    private void inject(String fieldName, Object value) throws Exception
    {
        Field field = LfScriptServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static LfScript copy(LfScript row)
    {
        if (row == null) { return null; }
        // 返回副本以模拟真实 Mapper，避免待校验的编辑对象提前修改已保存数据。
        LfScript copy = new LfScript();
        copy.setId(row.getId());
        copy.setApplicationName(row.getApplicationName());
        copy.setScriptId(row.getScriptId());
        copy.setScriptName(row.getScriptName());
        copy.setScriptData(row.getScriptData());
        copy.setScriptType(row.getScriptType());
        copy.setScriptLanguage(row.getScriptLanguage());
        copy.setEnable(row.getEnable());
        copy.setVersion(row.getVersion());
        return copy;
    }
}
