package cn.huanzi.qch.baseadmin;

import cn.huanzi.qch.baseadmin.common.pojo.Result;
import cn.huanzi.qch.baseadmin.sys.sysauthority.pojo.SysAuthority;
import cn.huanzi.qch.baseadmin.sys.sysauthority.repository.SysAuthorityRepository;
import cn.huanzi.qch.baseadmin.sys.sysmenu.pojo.SysMenu;
import cn.huanzi.qch.baseadmin.sys.sysmenu.repository.SysMenuRepository;
import cn.huanzi.qch.baseadmin.sys.sysmenu.vo.SysMenuVo;
import cn.huanzi.qch.baseadmin.sys.syssetting.service.SysSettingService;
import cn.huanzi.qch.baseadmin.sys.syssetting.vo.SysSettingVo;
import cn.huanzi.qch.baseadmin.sys.sysshortcutmenu.service.SysShortcutMenuService;
import cn.huanzi.qch.baseadmin.sys.sysshortcutmenu.vo.SysShortcutMenuVo;
import cn.huanzi.qch.baseadmin.sys.sysuser.pojo.SysUser;
import cn.huanzi.qch.baseadmin.sys.sysuser.repository.SysUserRepository;
import cn.huanzi.qch.baseadmin.sys.sysuser.service.SysUserService;
import cn.huanzi.qch.baseadmin.sys.sysuser.vo.SysUserVo;
import cn.huanzi.qch.baseadmin.sys.sysuserauthority.service.SysUserAuthorityService;
import cn.huanzi.qch.baseadmin.sys.sysusermenu.service.SysUserMenuService;
import cn.huanzi.qch.baseadmin.util.*;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@EnableAsync//??????????????????
@SpringBootApplication
@MapperScan(value = "cn.huanzi.qch.baseadmin.mbg.mapper")
public class BaseAdminApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(BaseAdminApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(BaseAdminApplication.class);
    }

    /**
     * ??????????????????session???????????????
     */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}

@Slf4j
@Controller
@RequestMapping("/")
@Configuration
class IndexController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysSettingService sysSettingService;

    @Autowired
    private SysUserMenuService sysUserMenuService;

    @Autowired
    private SysShortcutMenuService sysShortcutMenuService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;


    /**
     * ??????
     */
    @Value("${server.port}")
    private String port;

    /**
     * ????????????
     */
    @Bean
    public ApplicationRunner applicationRunner() {
        return applicationArguments -> {
            try {
                //??????????????????????????????????????????????????????????????????sysSettingMap
                SysSettingVo sysSettingVo = sysSettingService.get(1).getData();
                SysSettingUtil.setSysSettingMap(sysSettingVo);

                //??????????????????IP
                log.info("???????????????" + "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + contextPath);
            } catch (UnknownHostException e) {
                //????????????????????????
                log.error(ErrorUtil.errorInfoToString(e));
            }
        };
    }

    /**
     * ??????????????????
     */
    @GetMapping("loginPage")
    public ModelAndView login(){
        ModelAndView modelAndView = new ModelAndView("login");

        //????????????
        modelAndView.addObject("sys", SysSettingUtil.getSysSetting());

        //????????????
        String publicKey = RsaUtil.getPublicKey();
        log.info("???????????????" + publicKey);
        modelAndView.addObject("publicKey", publicKey);

        return modelAndView;
    }


    /**
     * ????????????
     */
    @GetMapping("")
    public void index1(HttpServletResponse response){
        System.out.println(1);
        //???????????????
        try {
            response.sendRedirect("/index");
        } catch (IOException e) {
            //????????????????????????
            log.error(ErrorUtil.errorInfoToString(e));
        }
    }
    @GetMapping("index")
    public ModelAndView index(){
        ModelAndView modelAndView = new ModelAndView("index");

        //????????????
        modelAndView.addObject("sys", SysSettingUtil.getSysSetting());

        //????????????
        SysUserVo sysUserVo = sysUserService.findByLoginName(SecurityUtil.getLoginUser().getUsername()).getData();
        sysUserVo.setPassword(null);//??????????????????
        modelAndView.addObject( "loginUser", sysUserVo);

        //????????????????????????
        List<SysMenuVo> menuVoList = sysUserMenuService.findByUserId(sysUserVo.getUserId()).getData();
        modelAndView.addObject("menuList",menuVoList);

        //????????????????????????
        List<SysShortcutMenuVo> shortcutMenuVoList= sysShortcutMenuService.findByUserId(sysUserVo.getUserId()).getData();
        modelAndView.addObject("shortcutMenuList",shortcutMenuVoList);

        //????????????
        String publicKey = RsaUtil.getPublicKey();
        log.info("???????????????" + publicKey);
        modelAndView.addObject("publicKey", publicKey);

        return modelAndView;
    }

    /**
     * ??????????????????????????????(???????????????????????????HttpSession???)
     */
    @RequestMapping("getVerifyCodeImage")
    public void getVerifyCodeImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //?????????????????????
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.getOutputStream();
        String verifyCode = VerifyCodeImageUtil.generateTextCode(VerifyCodeImageUtil.TYPE_NUM_UPPER, 4, null);

        //??????????????????HttpSession??????
        request.getSession().setAttribute("verifyCode", verifyCode);
         log.info("??????????????????????????????" + verifyCode + ",????????????HttpSession???");

        //?????????????????????????????????JPEG??????
        response.setContentType("image/jpeg");
        BufferedImage bufferedImage = VerifyCodeImageUtil.generateImageCode(verifyCode, 90, 30, 3, true, Color.WHITE, Color.BLACK, null);

        //???????????????
        ImageIO.write(bufferedImage, "JPEG", response.getOutputStream());
    }

    /**
     * ??????????????????????????????
     */
    @GetMapping("monitor")
    public ModelAndView monitor() {
        return new ModelAndView("monitor.html","port",port);
    }

    /**
     * ??????????????????
     */
    @GetMapping("logging")
    public ModelAndView logging() {
        return new ModelAndView("logging.html","port",port);
    }
}
