package com.aios.platform.system.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.system.entity.SysMenu;
import com.aios.platform.system.mapper.SysMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;

    public List<SysMenu> tree() {
        return build(listAll(), 0L);
    }

    /** 管理端：含停用菜单，便于维护完整层级 */
    public List<SysMenu> treeForManage() {
        return build(listAllForManage(), 0L);
    }

    public List<SysMenu> navTree(Set<String> permissionCodes, boolean superAdmin) {
        List<SysMenu> full = tree();
        if (superAdmin) {
            return full;
        }
        return filterMenus(full, permissionCodes);
    }

    public SysMenu get(Long id) {
        SysMenu m = menuMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("菜单不存在");
        }
        return m;
    }

    @Transactional
    public Long save(SysMenu body) {
        if (body.getMenuId() == null) {
            if (body.getParentId() == null) {
                body.setParentId(0L);
            }
            if (body.getVisible() == null) {
                body.setVisible(1);
            }
            if (body.getStatus() == null) {
                body.setStatus(1);
            }
            if (body.getSortOrder() == null) {
                body.setSortOrder(0);
            }
            menuMapper.insert(body);
            return body.getMenuId();
        }
        SysMenu db = get(body.getMenuId());
        db.setParentId(body.getParentId());
        db.setMenuName(body.getMenuName());
        db.setPath(body.getPath());
        db.setComponent(body.getComponent());
        db.setIcon(body.getIcon());
        db.setSortOrder(body.getSortOrder());
        db.setPermissionCode(body.getPermissionCode());
        db.setVisible(body.getVisible());
        db.setStatus(body.getStatus());
        menuMapper.updateById(db);
        return db.getMenuId();
    }

    @Transactional
    public void delete(Long id) {
        Long child =
                menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (child != null && child > 0) {
            throw new BusinessException("请先删除子菜单");
        }
        menuMapper.deleteById(id);
    }

    private List<SysMenu> listAll() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSortOrder));
    }

    private List<SysMenu> listAllForManage() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSortOrder));
    }

    private List<SysMenu> build(List<SysMenu> all, Long parentId) {
        List<SysMenu> out = new ArrayList<>();
        for (SysMenu m : all) {
            Long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (Objects.equals(pid, parentId)) {
                SysMenu copy = shallow(m);
                copy.setChildren(build(all, m.getMenuId()));
                out.add(copy);
            }
        }
        return out;
    }

    private List<SysMenu> filterMenus(List<SysMenu> nodes, Set<String> codes) {
        List<SysMenu> out = new ArrayList<>();
        for (SysMenu node : nodes) {
            List<SysMenu> children = filterMenus(
                    node.getChildren() != null ? node.getChildren() : List.of(), codes);
            boolean selfOk = canSee(node, codes);
            if (selfOk || !children.isEmpty()) {
                SysMenu copy = shallow(node);
                copy.setChildren(children);
                out.add(copy);
            }
        }
        return out;
    }

    private static boolean canSee(SysMenu m, Set<String> codes) {
        if (m.getVisible() != null && m.getVisible() == 0) {
            return false;
        }
        String perm = m.getPermissionCode();
        if (perm == null || perm.isBlank()) {
            return true;
        }
        return codes.contains(perm);
    }

    private static SysMenu shallow(SysMenu m) {
        SysMenu x = new SysMenu();
        x.setMenuId(m.getMenuId());
        x.setParentId(m.getParentId());
        x.setMenuName(m.getMenuName());
        x.setPath(m.getPath());
        x.setComponent(m.getComponent());
        x.setIcon(m.getIcon());
        x.setSortOrder(m.getSortOrder());
        x.setPermissionCode(m.getPermissionCode());
        x.setVisible(m.getVisible());
        x.setStatus(m.getStatus());
        return x;
    }
}
