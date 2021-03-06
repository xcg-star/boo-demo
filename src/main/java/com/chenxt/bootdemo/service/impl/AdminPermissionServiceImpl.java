package com.chenxt.bootdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenxt.bootdemo.base.expection.enumeration.BusinessExceptionCodeEnum;
import com.chenxt.bootdemo.base.security.Token;
import com.chenxt.bootdemo.base.third.auth2.GoogleAuthenticatorUtils;
import com.chenxt.bootdemo.base.util.ListUtils;
import com.chenxt.bootdemo.base.util.Md5Utils;
import com.chenxt.bootdemo.base.util.ValidateUtils;
import com.chenxt.bootdemo.dto.*;
import com.chenxt.bootdemo.entity.*;
import com.chenxt.bootdemo.mapper.*;
import com.chenxt.bootdemo.service.IAdminPermissionService;
import com.chenxt.bootdemo.vo.*;
import org.apache.commons.beanutils.BeanMap;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * 后台管理权限service接口
 *
 * @author chenxt
 * @date 2020/08/03
 */
@Service
public class AdminPermissionServiceImpl implements IAdminPermissionService {
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private AdminGroupUserLinkMapper adminGroupUserLinkMapper;
    @Resource
    private AdminPermissionLinkMapper adminPermissionLinkMapper;
    @Resource
    private AdminGroupMapper adminGroupMapper;
    @Resource
    private AdminMenuMapper adminMenuMapper;
    @Resource
    private AdminPermissionMapper adminPermissionMapper;

    private static final String SECRET_QR_CODE_ISSUER = "http://www.chenxt.com";

    @Override
    public Boolean adminLogin(AdminUserLoginDTO adminUserLoginDTO) {
        String account = adminUserLoginDTO.getAccount();
        String password = adminUserLoginDTO.getPassword();
        BusinessExceptionCodeEnum.INVALID_PARAMETER.assertIsFalse(ValidateUtils.isOneEmpty(account, password));
        AdminUser adminUser = adminUserMapper.selectByAccount(account);
        BusinessExceptionCodeEnum.ADMIN_USER_ACCOUNT_NOT_EXIST.assertNotNull(adminUser);
        //密码错误
        BusinessExceptionCodeEnum.ADMIN_USER_PASSWORD_WRONG.assertIsTrue(Md5Utils.verifyHex(adminUser.getPassword(), password));
        return true;
    }

    @Override
    public AdminUserVO authorize(AdminUserVerifyCodeDTO adminUserVerifyCodeDTO) {
        String account = adminUserVerifyCodeDTO.getAccount();
        String verifyCode = adminUserVerifyCodeDTO.getVerifyCode();
        AdminUser adminUser = adminUserMapper.selectByAccount(account);
        //账号不存在
        BusinessExceptionCodeEnum.ADMIN_USER_ACCOUNT_NOT_EXIST.assertNotNull(adminUser);
        long code = Long.parseLong(verifyCode);
//        BusinessExceptionCodeEnum.ADMIN_USER_VERIFYCODE_WRONG.assertIsTrue(GoogleAuthenticatorUtils.check_code(adminUser.getSecret(), code, System.currentTimeMillis()));
        AdminUserVO adminUserVO = new AdminUserVO();
        BeanUtils.copyProperties(adminUser, adminUserVO);
        //TODO
//        adminUserVO.setCdnUrl(cdnUrl);
//        adminUserVO.setCdnUploadUrl(cdnUploadUrl);
        //设置菜单权限列表
//        adminUserVO.setPermissionList(getPermissionForControlList(adminUser.getId(), adminUser.getType(), adminUser.getIsAdminEnable()));
        return adminUserVO;
    }

    @Override
    public AdminUser resetSecret(Long userId) {
        AdminUser adminUser = new AdminUser();
        adminUser.setId(userId);
        adminUser.setSecret(GoogleAuthenticatorUtils.getRandomSecretKey());
        adminUser.setSecretQrCode(GoogleAuthenticatorUtils.getGoogleAuthenticatorBarCode(adminUser.getSecret(), adminUser.getAccount(), SECRET_QR_CODE_ISSUER));
        adminUserMapper.updateById(adminUser);
        return adminUser;
    }

    @Override
    public Boolean addUser(AdminUserDTO adminUserDTO) {
        BusinessExceptionCodeEnum.INVALID_PARAMETER.assertIsFalse(ValidateUtils.isOneEmpty(adminUserDTO.getName(), adminUserDTO.getAccount(), adminUserDTO.getPassword()));
        //账号已存在
        BusinessExceptionCodeEnum.ADMIN_USER_ACCOUNT_EXIST.assertIsNull(adminUserMapper.selectByAccount(adminUserDTO.getAccount()));
        //用户名已存在
        BusinessExceptionCodeEnum.ADMIN_USER_NAME_EXIST.assertIsNull(adminUserMapper.selectByName(adminUserDTO.getName()));
        AdminUser adminUser = new AdminUser();
        BeanUtils.copyProperties(adminUserDTO, adminUser);
        //密码使用Md5加密
        adminUser.setPassword(Md5Utils.encodeHex(adminUser.getPassword()));
        //设置密钥和密钥二维码
        adminUser.setSecret(GoogleAuthenticatorUtils.getRandomSecretKey());
        adminUser.setSecretQrCode(GoogleAuthenticatorUtils.getGoogleAuthenticatorBarCode(adminUser.getSecret(), adminUser.getAccount(), SECRET_QR_CODE_ISSUER));
        adminUserMapper.insert(adminUser);
        return true;
    }

    @Override
    public Boolean isUserNameExist(String name) {
        return adminUserMapper.selectByName(name) != null;
    }

    @Override
    public Boolean isUserAccountExist(String account) {
        return adminUserMapper.selectByAccount(account) != null;
    }

    @Override
    public void updateUser(AdminUserUpdateDTO adminUserUpdateDTO) {
        AdminUser adminUser = adminUserMapper.selectByName(adminUserUpdateDTO.getName());
        //用户名已存在
        BusinessExceptionCodeEnum.ADMIN_USER_NAME_EXIST.assertIsTrue(adminUser == null || adminUser.getId().equals(adminUserUpdateDTO.getId()));
        adminUser = new AdminUser();
        BeanUtils.copyProperties(adminUserUpdateDTO, adminUser);
        if (adminUser.getPassword() != null) {
            adminUser.setPassword(Md5Utils.encodeHex(adminUser.getPassword()));
        }
        adminUserMapper.updateById(adminUser);
    }

    @Override
    public void updatePassword(AdminPasswordUpdateDTO adminPasswordUpdateDTO, Token token) {
        Long currentUserId = token.getCurrentUserId();
        AdminUser adminUser = adminUserMapper.selectById(currentUserId);
        //用户不存在
        BusinessExceptionCodeEnum.ADMIN_USER_NOT_EXIST.assertNotNull(adminUser);
        //密码错误
        BusinessExceptionCodeEnum.ADMIN_USER_PASSWORD_WRONG.assertIsTrue(Md5Utils.verifyHex(adminUser.getPassword(), adminPasswordUpdateDTO.getOldPassword()));
        adminUser = new AdminUser();
        adminUser.setId(currentUserId);
        adminUser.setPassword(Md5Utils.encodeHex(adminPasswordUpdateDTO.getNewPassword()));
        adminUserMapper.updateById(adminUser);
    }

    @Override
    public void deleteUser(Long id) {
        adminUserMapper.deleteById(id);
        adminGroupUserLinkMapper.deleteByUserId(id);
        adminPermissionLinkMapper.deleteByUserId(id);
    }

    @Override
    public IPage<AdminUserListVO> getUserList(AdminUserListDTO adminUserListDTO) {
        //查询全部用户组
        List<AdminGroup> adminGroupList = adminGroupMapper.selectList(null);
        //查询全部用户组用户关系
        List<AdminGroupUserLink> adminGroupUserLinkList = adminGroupUserLinkMapper.selectList(null);
        //分页条件查询
        Page<AdminUser> userPage = new Page<>(adminUserListDTO.getPage(), adminUserListDTO.getSize());
        BeanMap queryParam = new BeanMap(adminUserListDTO);
        IPage<AdminUser> adminUserIPage = adminUserMapper.selectPageVo(userPage, queryParam);
        //将Page<AdminUser>转为Page<AdminUserVO>
        return adminUserIPage.convert(adminUser -> {
            AdminUserListVO adminUserListVO = new AdminUserListVO();
            BeanUtils.copyProperties(adminUser, adminUserListVO);
            List<Long> groupIdList = adminGroupUserLinkList.stream().filter(adminGroupUserLink -> adminGroupUserLink.getAdminUserId().equals(adminUser.getId())).map(AdminGroupUserLink::getAdminGroupId).collect(Collectors.toList());
            if (!groupIdList.isEmpty()) {
                List<AdminUserGroupVO> adminUserGroupVOList = adminGroupList.stream().filter(adminGroup -> groupIdList.contains(adminGroup.getId())).map(adminGroup -> {
                    AdminUserGroupVO adminUserGroupVO = new AdminUserGroupVO();
                    adminUserGroupVO.setGroupId(adminGroup.getId());
                    adminUserGroupVO.setGroupName(adminGroup.getName());
                    return adminUserGroupVO;
                }).collect(Collectors.toList());
                adminUserListVO.setGroupList(adminUserGroupVOList);
            }
            return adminUserListVO;
        });
    }

    @Override
    public Long addMenu(AdminMenuDTO adminMenuDTO) {
        BusinessExceptionCodeEnum.ADMIN_PARENT_MENU_NOT_EXIST.assertIsTrue(adminMenuDTO.getParentId() == null || adminMenuMapper.selectById(adminMenuDTO.getParentId()) != null);
        BusinessExceptionCodeEnum.ADMIN_MENU_NAME_EXIST.assertIsNull(adminMenuMapper.selectByNameAndParentId(adminMenuDTO.getName(), adminMenuDTO.getParentId()));
        AdminMenu adminMenu = new AdminMenu();
        BeanUtils.copyProperties(adminMenuDTO, adminMenu);
        adminMenuMapper.insert(adminMenu);
        //新增菜单对应的权限
        AdminPermission adminPermission = new AdminPermission();
        adminPermission.setAdminMenuId(adminMenu.getId());
        adminPermissionMapper.insert(adminPermission);
        return adminMenu.getId();
    }

    @Override
    public void updateMenu(AdminMenuUpdateDTO adminMenuUpdateDTO) {
        AdminMenu adminMenu = adminMenuMapper.selectById(adminMenuUpdateDTO.getId());
        BusinessExceptionCodeEnum.ADMIN_MENU_NOT_EXIST.assertNotNull(adminMenu);
        adminMenu = new AdminMenu();
        BeanUtils.copyProperties(adminMenuUpdateDTO, adminMenu);
        adminMenuMapper.updateById(adminMenu);
    }

    @Override
    public void deleteMenu(Long id) {
        BusinessExceptionCodeEnum.ADMIN_MENU_NOT_EXIST.assertNotNull(adminMenuMapper.selectById(id));
        BusinessExceptionCodeEnum.ADMIN_MENU_HAS_CHILDREN.assertIsFalse(adminMenuMapper.hasChildren(id));
        adminMenuMapper.deleteById(id);
        //删除菜单对应的权限
        AdminPermission adminPermission = adminPermissionMapper.selectByMenuId(id);
        adminPermissionMapper.deleteById(adminPermission.getId());
        //删除权限对应的权限关系
        adminPermissionLinkMapper.deleteByPermissionId(adminPermission.getId());
    }

    @Override
    public List<AdminMenuVO> getMenuList() {
        return ListUtils.copyListBeanProperties(getAdminMenuListOrder(false), AdminMenuVO.class);
    }

    @Override
    public Long addGroup(AdminGroupDTO adminGroupDTO) {
        BusinessExceptionCodeEnum.ADMIN_GROUP_EXIST.assertIsNull(adminGroupMapper.selectByName(adminGroupDTO.getName()));
        AdminGroup adminGroup = new AdminGroup();
        BeanUtils.copyProperties(adminGroupDTO, adminGroup);
        adminGroupMapper.insert(adminGroup);
        return adminGroup.getId();
    }

    @Override
    public void updateGroup(AdminGroupUpdateDTO adminGroupUpdateDTO) {
        AdminGroup adminGroup = adminGroupMapper.selectByName(adminGroupUpdateDTO.getName());
        BusinessExceptionCodeEnum.ADMIN_GROUP_EXIST.assertIsTrue(adminGroup == null || adminGroup.getId().equals(adminGroupUpdateDTO.getId()));
        adminGroup = new AdminGroup();
        BeanUtils.copyProperties(adminGroupUpdateDTO, adminGroup);
        adminGroupMapper.updateById(adminGroup);
    }

    @Override
    public void deleteGroup(Long id) {
        BusinessExceptionCodeEnum.ADMIN_GROUP_HAS_USER.assertIsTrue(CollectionUtils.isEmpty(adminGroupUserLinkMapper.selectByGroupId(id)));
        adminGroupMapper.deleteById(id);
        adminPermissionLinkMapper.deleteByGroupId(id);
    }

    @Override
    public IPage<AdminGroupListVO> getGroupList(AdminGroupListDTO adminGroupListDTO) {
        //分页条件查询
        Page<AdminGroup> adminGroupPage = new Page<>(adminGroupListDTO.getPage(), adminGroupListDTO.getSize());
        BeanMap queryParam = new BeanMap(adminGroupListDTO);
        IPage<AdminGroup> adminGroupIPage = adminGroupMapper.selectPageVo(adminGroupPage, queryParam);
        //将Page<AdminGroup>转为Page<AdminGroupListVO>
        return adminGroupIPage.convert(adminGroup -> {
            AdminGroupListVO adminGroupListVO = new AdminGroupListVO();
            BeanUtils.copyProperties(adminGroup, adminGroupListVO);
            adminGroupListVO.setUserCount(adminGroupUserLinkMapper.countByGroupId(adminGroup.getId()));
            return adminGroupListVO;
        });
    }

    @Override
    public void updateUserGroup(Long userId, AdminUserGroupDTO adminUserGroupDTO) {
        //查询全部用户组
        List<AdminGroup> adminGroupList = adminGroupMapper.selectList(null);

        adminUserGroupDTO.getGroupIdList().forEach(groupId -> {
            BusinessExceptionCodeEnum.ADMIN_GROUP_ID_NOT_EXIST.assertIsTrue(adminGroupList.stream().anyMatch(adminGroup -> adminGroup.getId().equals(groupId)));
        });
        adminGroupUserLinkMapper.deleteByUserIdAndNotInGroupIdList(adminUserGroupDTO.getGroupIdList(), userId);
        if (!CollectionUtils.isEmpty(adminUserGroupDTO.getGroupIdList())) {
            adminGroupUserLinkMapper.insertByGroupIdListAndUserIdOnDuplicateDoNoting(adminUserGroupDTO.getGroupIdList(), userId);
        }
    }

    @Override
    public void deleteUserGroup(Long userId, Long groupId) {
        adminGroupUserLinkMapper.delete(new LambdaQueryWrapper<AdminGroupUserLink>()
                .eq(AdminGroupUserLink::getAdminUserId, userId)
                .eq(AdminGroupUserLink::getAdminGroupId, groupId));
    }

    @Override
    public IPage<AdminUserGroupVO> getUserGroupList(Long userId, Integer page, Integer size) {
        //查询全部用户组
        List<AdminGroup> adminGroupList = adminGroupMapper.selectList(null);

        Page<AdminGroupUserLink> adminGroupUserLinkPage = new Page<>(page, size);
        IPage<AdminGroupUserLink> adminGroupUserLinkIPage = adminGroupUserLinkMapper.selectPageVoByUserId(adminGroupUserLinkPage, userId);
        //将Page<AdminGroupUserLink>转为Page<AdminUserGroupVO>
        return adminGroupUserLinkIPage.convert(adminGroupUserLink -> {
            AdminUserGroupVO adminUserGroupVO = new AdminUserGroupVO();
            adminUserGroupVO.setGroupId(adminGroupUserLink.getAdminGroupId());
            adminGroupList.stream().filter(adminGroup -> adminGroup.getId().equals(adminGroupUserLink.getAdminGroupId())).findFirst().ifPresent(adminGroup -> adminUserGroupVO.setGroupName(adminGroup.getName()));
            BusinessExceptionCodeEnum.ADMIN_GROUP_ID_NOT_EXIST.assertNotNull(adminUserGroupVO.getGroupName());

            adminUserGroupVO.setJoinTime(adminGroupUserLink.getCreatedAt());
            return adminUserGroupVO;
        });
    }

    @Override
    public List<AdminUserGroupVO> getUserAllGroupList(Long userId) {
        //查询全部用户组
        List<AdminGroup> adminGroupList = adminGroupMapper.selectList(null);

        List<AdminGroupUserLink> adminGroupUserLinkList = adminGroupUserLinkMapper.selectByUserId(userId);
        return adminGroupUserLinkList.stream().map(adminGroupUserLink -> {
            AdminUserGroupVO adminUserGroupVO = new AdminUserGroupVO();
            adminUserGroupVO.setGroupId(adminGroupUserLink.getAdminGroupId());
            adminGroupList.stream().filter(adminGroup -> adminGroup.getId().equals(adminGroupUserLink.getAdminGroupId())).findFirst().ifPresent(adminGroup -> adminUserGroupVO.setGroupName(adminGroup.getName()));
            BusinessExceptionCodeEnum.ADMIN_GROUP_ID_NOT_EXIST.assertNotNull(adminUserGroupVO.getGroupName());
            adminUserGroupVO.setJoinTime(adminGroupUserLink.getCreatedAt());
            return adminUserGroupVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateGroupUser(Long groupId, AdminGroupUserDTO adminGroupUserDTO) {
        //查询全部用户
        List<AdminUser> adminUserList = adminUserMapper.selectList(null);

        adminGroupUserDTO.getUserIdList().forEach(userId -> BusinessExceptionCodeEnum.ADMIN_USER_ID_NOT_EXIST.assertIsFalse(adminUserList.stream().noneMatch(adminUser -> adminUser.getId().equals(userId))));
        adminGroupUserLinkMapper.deleteByGroupIdAndNotInUserIdList(groupId, adminGroupUserDTO.getUserIdList());
        if (!CollectionUtils.isEmpty(adminGroupUserDTO.getUserIdList())) {
            adminGroupUserLinkMapper.insertByGroupIdAndUserIdListOnDuplicateDoNoting(groupId, adminGroupUserDTO.getUserIdList());
        }
    }

    @Override
    public void deleteGroupUser(Long groupId, Long userId) {
        adminGroupUserLinkMapper.delete(new LambdaQueryWrapper<AdminGroupUserLink>()
                .eq(AdminGroupUserLink::getAdminUserId, userId)
                .eq(AdminGroupUserLink::getAdminGroupId, groupId));
    }

    @Override
    public IPage<AdminGroupUserVO> getGroupUserList(Long groupId, Integer page, Integer size) {
        //查询全部用户
        List<AdminUser> adminUserList = adminUserMapper.selectList(null);

        Page<AdminGroupUserLink> adminGroupUserLinkPage = new Page<>(page, size);
        IPage<AdminGroupUserLink> adminGroupUserLinkIPage = adminGroupUserLinkMapper.selectPageVoByGroupId(adminGroupUserLinkPage, groupId);
        //将Page<AdminGroupUserLink>转为Page<AdminUserGroupVO>
        return adminGroupUserLinkIPage.convert(adminGroupUserLink -> {
            AdminGroupUserVO adminGroupUserVO = new AdminGroupUserVO();
            adminGroupUserVO.setUserId(adminGroupUserLink.getAdminUserId());
            adminUserList.stream().filter(adminUser -> adminUser.getId().equals(adminGroupUserLink.getAdminUserId())).findFirst().ifPresent(adminUser -> adminGroupUserVO.setUserName(adminUser.getName()));
            BusinessExceptionCodeEnum.ADMIN_USER_ID_NOT_EXIST.assertNotNull(adminGroupUserVO.getUserName());

            adminGroupUserVO.setJoinTime(adminGroupUserLink.getCreatedAt());
            return adminGroupUserVO;
        });
    }

    @Override
    public IPage<AdminUserPermissionVO> getUserPermissionList(Long userId, Integer page, Integer size) {
        //查询全部菜单
        List<AdminMenu> adminMenuList = adminMenuMapper.selectList(null);
        Map<Long, AdminMenu> adminMenuMap = adminMenuList.stream().collect(toMap(AdminMenu::getId, adminMenu -> adminMenu));
        //查询全部权限
        List<AdminPermission> adminPermissionList = adminPermissionMapper.selectList(null);
        Map<Long, AdminPermission> adminPermissionMap = adminPermissionList.stream().collect(toMap(AdminPermission::getId, adminPermission -> adminPermission));

        Page<AdminPermissionLink> adminPermissionLinkPage = new Page<>(page, size);
        IPage<AdminPermissionLink> adminPermissionLinkIPage = adminPermissionLinkMapper.selectPageVoByUserId(adminPermissionLinkPage, userId);
        //将Page<AdminPermissionLink>转为Page<AdminUserPermissionVO>
        return adminPermissionLinkIPage.convert(adminPermissionLink -> {
            AdminUserPermissionVO adminUserPermissionVO = new AdminUserPermissionVO();
            AdminPermission permission = adminPermissionMap.get(adminPermissionLink.getAdminPermissionId());
            //权限不存在
            BusinessExceptionCodeEnum.ADMIN_PERMISSION_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu menu = adminMenuMap.get(permission.getAdminMenuId());
            //菜单不存在
            BusinessExceptionCodeEnum.ADMIN_MENU_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu parentMenu = adminMenuMap.get(menu.getParentId());

            adminUserPermissionVO.setPermissionId(permission.getId());
            adminUserPermissionVO.setMenuId(menu.getId());
            adminUserPermissionVO.setMenuName(menu.getName());
            if (parentMenu != null) {
                //是子级
                adminUserPermissionVO.setParentMenuId(parentMenu.getId());
                adminUserPermissionVO.setParentMenuName(parentMenu.getName());
            }
            adminUserPermissionVO.setIsButtonEnable(adminPermissionLink.getIsButtonEnable());
            adminUserPermissionVO.setAddTime(adminPermissionLink.getCreatedAt());
            return adminUserPermissionVO;
        });
    }

    @Override
    public IPage<AdminUserPermissionExtendVO> getUserPermissionExtendList(Long userId, Integer page, Integer size, Long groupId) {
        //查询全部菜单
        List<AdminMenu> adminMenuList = adminMenuMapper.selectList(null);
        Map<Long, AdminMenu> adminMenuMap = adminMenuList.stream().collect(toMap(AdminMenu::getId, adminMenu -> adminMenu));
        //查询全部权限
        List<AdminPermission> adminPermissionList = adminPermissionMapper.selectList(null);
        Map<Long, AdminPermission> adminPermissionMap = adminPermissionList.stream().collect(toMap(AdminPermission::getId, adminPermission -> adminPermission));
        //查询全部用户组
        List<AdminGroup> adminGroupList = adminGroupMapper.selectList(null);
        Map<Long, AdminGroup> adminGroupMap = adminGroupList.stream().collect(toMap(AdminGroup::getId, adminGroup -> adminGroup));

        Page<AdminPermissionLink> adminPermissionLinkPage = new Page<>(page, size);
        IPage<AdminPermissionLink> adminPermissionLinkIPage = adminPermissionLinkMapper.selectExtendPageVoByUserId(adminPermissionLinkPage, userId, groupId);
        //将Page<AdminPermissionLink>转为Page<AdminUserPermissionVO>
        return adminPermissionLinkIPage.convert(adminPermissionLink -> {
            AdminUserPermissionExtendVO adminUserPermissionExtendVO = new AdminUserPermissionExtendVO();
            AdminPermission permission = adminPermissionMap.get(adminPermissionLink.getAdminPermissionId());
            //权限不存在
            BusinessExceptionCodeEnum.ADMIN_PERMISSION_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu menu = adminMenuMap.get(permission.getAdminMenuId());
            //菜单不存在
            BusinessExceptionCodeEnum.ADMIN_MENU_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu parentMenu = adminMenuMap.get(menu.getParentId());

            AdminGroup adminGroup = adminGroupMap.get(adminPermissionLink.getAdminGroupId());
            //用户组不存在
            BusinessExceptionCodeEnum.ADMIN_GROUP_ID_NOT_EXIST.assertNotNull(adminGroup);
            adminUserPermissionExtendVO.setGroupId(adminGroup.getId());
            adminUserPermissionExtendVO.setGroupName(adminGroup.getName());
            adminUserPermissionExtendVO.setPermissionId(permission.getId());
            adminUserPermissionExtendVO.setMenuId(menu.getId());
            adminUserPermissionExtendVO.setMenuName(menu.getName());
            if (parentMenu != null) {
                //是子级
                adminUserPermissionExtendVO.setParentMenuId(parentMenu.getId());
                adminUserPermissionExtendVO.setParentMenuName(parentMenu.getName());
            }
            adminUserPermissionExtendVO.setIsButtonEnable(adminPermissionLink.getIsButtonEnable());
            adminUserPermissionExtendVO.setAddTime(adminPermissionLink.getCreatedAt());
            return adminUserPermissionExtendVO;
        });
    }

    @Override
    public List<AdminUserPermissionAllVO> getUserPermissionAllList(Long userId) {

        //查询全部菜单
        List<AdminMenu> adminMenuList = adminMenuMapper.selectList(null);
        Map<Long, AdminMenu> adminMenuMap = adminMenuList.stream().collect(toMap(AdminMenu::getId, adminMenu -> adminMenu));
        //查询全部权限
        List<AdminPermission> adminPermissionList = adminPermissionMapper.selectList(null);
        Map<Long, AdminPermission> adminPermissionMap = adminPermissionList.stream().collect(toMap(AdminPermission::getId, adminPermission -> adminPermission));

        //继承权限id列表
        List<Long> permissionIdOnlyWithExtendList = adminPermissionLinkMapper.selectPermissionIdListOnlyWithExtendByUserId(userId);
        List<AdminPermissionLink> permissionLinkWithExtendList = adminPermissionLinkMapper.selectPermissionIdListWithExtendByUserId(userId);
        //将Page<AdminPermissionLink>转为Page<AdminUserPermissionVO>
        return permissionLinkWithExtendList.stream().map(adminPermissionLink -> {
            AdminUserPermissionAllVO adminUserPermissionAllVO = new AdminUserPermissionAllVO();
            AdminPermission permission = adminPermissionMap.get(adminPermissionLink.getAdminPermissionId());
            //权限不存在
            BusinessExceptionCodeEnum.ADMIN_PERMISSION_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu menu = adminMenuMap.get(permission.getAdminMenuId());
            //菜单不存在
            BusinessExceptionCodeEnum.ADMIN_MENU_ID_NOT_EXIST.assertNotNull(permission);
            AdminMenu parentMenu = adminMenuMap.get(menu.getParentId());

            adminUserPermissionAllVO.setPermissionId(permission.getId());
            adminUserPermissionAllVO.setMenuId(menu.getId());
            adminUserPermissionAllVO.setMenuName(menu.getName());
            if (parentMenu != null) {
                //是子级
                adminUserPermissionAllVO.setParentMenuId(parentMenu.getId());
                adminUserPermissionAllVO.setParentMenuName(parentMenu.getName());
            }
            adminUserPermissionAllVO.setIsButtonEnable(adminPermissionLink.getIsButtonEnable());
            adminUserPermissionAllVO.setAddTime(adminPermissionLink.getCreatedAt());
            adminUserPermissionAllVO.setIsExtend(permissionIdOnlyWithExtendList.contains(adminPermissionLink.getAdminPermissionId()));
            return adminUserPermissionAllVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateUserPermission(Long userId, AdminUserPermissionDTO adminUserPermissionDTO) {

    }

    @Override
    public void deleteUserPermission(Long userId, Long permissionId) {

    }

    @Override
    public IPage<AdminGroupPermissionVO> getGroupPermissionList(Long groupId, Integer page, Integer size) {
        return null;
    }

    @Override
    public void updateGroupPermission(Long groupId, AdminGroupPermissionDTO adminGroupPermissionDTO) {

    }

    @Override
    public void deleteGroupPermission(Long groupId, Long permissionId) {

    }

    @Override
    public AdminUserListVO getUserDetail(Long userId) {
        return null;
    }

    @Override
    public AdminGroupListVO getGroupDetail(Long groupId) {
        return null;
    }

    @Override
    public List<AdminPermissionVO> getPermissionList() {
        return null;
    }

    @Override
    public IPage<AdminUserListVO> getAdministratorList(Integer page, Integer size, String name) {
        return null;
    }

    @Override
    public void addAdministrator(Long userId) {

    }

    @Override
    public void deleteAdministrator(Long userId) {

    }

    @Override
    public void updateAdministratorStatus(Long userId, AdminEnableDTO adminEnableDTO) {

    }

    @Override
    public List<AdminUserListVO> getCommonUserList(String userName) {
        return null;
    }

    @Override
    public List<AdminUserPermissionForControlVO> getUserPermissionForControlList(Token token) {
        return null;
    }

    @Override
    public List<AdminUserListVO> getGroupUserDropDownList(Long groupId, String name, Token token) {
        return null;
    }

    @Override
    public List<AdminGroupListVO> getUserGroupDropDownList(Long userId, String name) {
        return null;
    }

    @Override
    public AdminUserVO getAdminUser(Long adminUserId) {
        return null;
    }

    private List<AdminMenu> getAdminMenuListOrder(Boolean mustEnable) {
        List<AdminMenu> parentList = adminMenuMapper.selectParentListOrder(mustEnable);
        List<AdminMenu> childrenList = adminMenuMapper.selectChildrenListOrder(mustEnable);
        List<AdminMenu> adminMenuList = new ArrayList<>();
        parentList.forEach(parent -> {
            adminMenuList.add(parent);
            adminMenuList.addAll(childrenList.stream().filter(child -> parent.getId().equals(child.getParentId())).collect(Collectors.toList()));
        });
        return adminMenuList;
    }
}
