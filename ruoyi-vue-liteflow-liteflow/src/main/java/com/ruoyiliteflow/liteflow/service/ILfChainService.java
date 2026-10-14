package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainExportVo;

public interface ILfChainService
{
    LfChain selectLfChainById(Long id);

    LfChain selectLfChainByName(String chainName);

    List<LfChain> selectLfChainList(LfChain lfChain);

    /**
     * 保存链路及审计记录；已发布且启用的链路加载失败时回滚保存。
     *
     * @param lfChain 待保存的链路定义
     * @return 新增记录数
     */
    int insertLfChain(LfChain lfChain);

    /**
     * 更新链路及审计记录；加载失败时保留原记录和运行规则。
     *
     * @param lfChain 待更新的链路定义
     * @return 更新记录数
     */
    int updateLfChain(LfChain lfChain);

    int deleteLfChainByIds(Long[] ids, String operateBy);

    /**
     * 独立编译主体与路由后加载链路，编译失败时保留原运行版本。
     * 当前调用处于数据库事务时，事务未提交也会恢复加载前的运行状态。
     *
     * @param lfChain 待加载的链路定义
     */
    void reloadChain(LfChain lfChain);

    /**
     * 按名称读取并重新加载链路，不改变发布状态或版本号。
     *
     * @param chainName 链路名称
     */
    void reloadChainByName(String chainName);

    /**
     * 编译通过后保存发布状态、审计与版本快照。
     * 加载或持久化失败时保留原发布状态、版本及运行规则。
     *
     * @param id 链路记录主键
     * @param publishBy 发布操作者
     */
    void publishChain(Long id, String publishBy);

    LfChain cloneChain(Long id, String newChainName, String newChainDesc, String createBy);

    LfChainExportVo exportChain(Long id);

    LfChain importChain(LfChainExportVo exportVo, String createBy);
}
