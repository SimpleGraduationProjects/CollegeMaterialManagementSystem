package cn.huanzi.qch.baseadmin.sys.sysuser.controller;

import cn.huanzi.qch.baseadmin.annotation.Decrypt;
import cn.huanzi.qch.baseadmin.annotation.Encrypt;
import cn.huanzi.qch.baseadmin.common.controller.CommonController;
import cn.huanzi.qch.baseadmin.common.pojo.PageInfo;
import cn.huanzi.qch.baseadmin.common.pojo.Result;
import cn.huanzi.qch.baseadmin.sys.sysauthority.pojo.SysAuthority;
import cn.huanzi.qch.baseadmin.sys.sysauthority.repository.SysAuthorityRepository;
import cn.huanzi.qch.baseadmin.sys.sysmenu.pojo.SysMenu;
import cn.huanzi.qch.baseadmin.sys.sysmenu.repository.SysMenuRepository;
import cn.huanzi.qch.baseadmin.sys.syssetting.service.SysSettingService;
import cn.huanzi.qch.baseadmin.sys.sysuser.pojo.SysUser;
import cn.huanzi.qch.baseadmin.sys.sysuser.repository.SysUserRepository;
import cn.huanzi.qch.baseadmin.sys.sysuser.service.SysUserService;
import cn.huanzi.qch.baseadmin.sys.sysuser.vo.SysUserVo;
import cn.huanzi.qch.baseadmin.sys.sysuserauthority.pojo.SysUserAuthority;
import cn.huanzi.qch.baseadmin.sys.sysuserauthority.repository.SysUserAuthorityRepository;
import cn.huanzi.qch.baseadmin.sys.sysusermenu.service.SysUserMenuService;
import cn.huanzi.qch.baseadmin.util.SysSettingUtil;
import cn.huanzi.qch.baseadmin.util.SysUserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys/sysUser/")
public class SysUserController extends CommonController<SysUserVo, SysUser, Integer> {
    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private SysSettingService sysSettingService;

    @Autowired
    private SysUserAuthorityRepository userAuthorityRepository;
    @Autowired
    private SysAuthorityRepository authorityRepository;


    @GetMapping("user")
    public ModelAndView user(){
        ModelAndView mv = new ModelAndView("sys/user/user", "initPassword", SysSettingUtil.getSysSetting().getUserInitPassword());
        mv.addObject("roleName", SysUserUtil.getSysUser().getRoleName());
        return mv;
    }

    @PostMapping("pageList")
    @Decrypt
    @Encrypt
    public Result<PageInfo<SysUserVo>> pageList(SysUserVo userVo) {
        Result<PageInfo<SysUserVo>> page = sysUserService.page(userVo);
        PageInfo<SysUserVo> data = page.getData();
        List<SysUserVo> rows = data.getRows();
        if(rows != null && rows.size() > 0) {
            rows.forEach(user -> {
                Integer userId = user.getUserId();
                SysUserAuthority userAuthority = userAuthorityRepository.findFirstByUserId(userId);
                if(userAuthority != null) {
                    Optional<SysAuthority> optionalSysAuthority = authorityRepository.findById(userAuthority.getAuthorityId());
                    optionalSysAuthority.ifPresent(sysAuthority -> user.setRoleName(sysAuthority.getAuthorityRemark()));
                }
            });
        }
        return page;
    }

    @PostMapping("resetPassword")
    @Decrypt
    @Encrypt
    public Result<SysUserVo> resetPassword(SysUserVo sysUserVo){
        return sysUserService.resetPassword(sysUserVo.getUserId());
    }

    @PostMapping("pageOnLine")
    @Decrypt
    @Encrypt
    public Result<PageInfo<SysUserVo>> pageOnLine(SysUserVo sysUserVo){
        ArrayList<SysUserVo> sysUserVoList = new ArrayList<>();
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
        for (Object allPrincipal : allPrincipals) {
            SysUserVo userVo = new SysUserVo();
            User user = (User) allPrincipal;
            userVo.setLoginName(user.getUsername());
            sysUserVoList.add(userVo);
        }
        PageInfo<SysUserVo> pageInfo = new PageInfo<>();
        pageInfo.setPage(1);//??????
        pageInfo.setPageSize(10000);//????????????
        pageInfo.setRows(sysUserVoList);//????????????
        pageInfo.setRecords(sysUserVoList.size());//????????????
        pageInfo.setTotal(1);//?????????
        return Result.of(pageInfo);
    }

    @DeleteMapping("forced/{loginName}")
    public Result<String> forced( @PathVariable("loginName") String loginName) {
        //??????remember-me?????????tokens
        sysUserService.getPersistentTokenRepository2().removeUserTokens(loginName);

        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
        for (Object allPrincipal : allPrincipals) {
            User user = (User) allPrincipal;
            if(user.getUsername().equals(loginName)){
                List<SessionInformation> allSessions = sessionRegistry.getAllSessions(user, true);
                if (allSessions != null) {
                    for (SessionInformation sessionInformation : allSessions) {
                        sessionInformation.expireNow();
                        sessionRegistry.removeSessionInformation(sessionInformation.getSessionId());
                    }
                }
                break;
            }
        }
        return Result.of("????????????");
    }

    @PostMapping("/uploadImage")
    @ResponseBody
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        File fileTemp =  null;
        try {
            if (file.isEmpty()) {
                result.put("code", "1");
                result.put("desc", "??????????????????");
                result.put("imageUrl", null);
                return result;
            }
            String contentType = file.getContentType();
            long size = file.getSize();
            if (!"image/jpg".equals(contentType) && !"image/png".equals(contentType)
                    && !"image/jpeg".equals(contentType)) {
                result.put("code", "1");
                result.put("desc", "??????????????????");
                result.put("imageUrl", null);
                return result;
            }
            String fileName = file.getOriginalFilename();
            String filePath = "D:\\pki\\";
            File dest = new File(filePath + fileName);
            if(!dest.getParentFile().exists()){
                //????????????????????????
                dest.getParentFile().mkdir();
            }

            file.transferTo(dest);
            String fileUrl = dest.getAbsolutePath();
            if (StringUtils.isEmpty(dest.getAbsolutePath())) {
                result.put("code", "1");
                result.put("desc", "???????????????????????????????????????");
                result.put("imageUrl", fileUrl);
                return result;
            }
            result.put("code", "0");
            result.put("desc", "????????????");
            result.put("imageUrl", "/image/"+fileName);
        } catch (Exception e) {
            result.put("code", "1");
            result.put("desc", "???????????????????????????");
            result.put("imageUrl", null);
        }
        return result;
    }
}
