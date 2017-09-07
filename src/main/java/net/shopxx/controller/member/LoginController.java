/*
 * Copyright 2005-2017 shopxx.net. All rights reserved.
 * Support: http://www.shopxx.net
 * License: http://www.shopxx.net/license
 */
package net.shopxx.controller.member;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.shopxx.entity.Member;
import net.shopxx.entity.SocialUser;
import net.shopxx.security.CurrentUser;
import net.shopxx.security.UserAuthenticationToken;
import net.shopxx.service.PluginService;
import net.shopxx.service.SocialUserService;
import net.shopxx.service.UserService;
import net.shopxx.util.TimeUtil;
import net.shopxx.util.WebUtils;

/**
 * Controller - 会员登录
 * 
 * @author SHOP++ Team
 * @version 5.0.3
 */
@Controller("memberLoginController")
@RequestMapping("/member/login")
public class LoginController extends BaseController {

	/**
	 * "重定向令牌"Cookie名称
	 */
	private static final String REDIRECT_TOKEN_COOKIE_NAME = "redirectToken";

	@Value("${member_index}")
	private String memberIndex;
	@Value("${member_login_view}")
	private String memberLoginView;
	@Inject
	private PluginService pluginService;
	@Inject
	private SocialUserService socialUserService;
	/**
	 * MD5加密约定码
	 */
	@Value("${url.signature}")
	private String urlSignature;
	@Inject
	private UserService userService;
	/**
	 * 登录页面
	 */
	@GetMapping
	public String index(String redirectUrl, String redirectToken, Long socialUserId, String uniqueId, @CurrentUser Member currentUser, HttpServletRequest request, HttpServletResponse response, ModelMap model) {
		if (StringUtils.isNotEmpty(redirectUrl) && StringUtils.isNotEmpty(redirectToken) && StringUtils.equals(redirectToken, WebUtils.getCookie(request, REDIRECT_TOKEN_COOKIE_NAME))) {
			model.addAttribute("redirectUrl", redirectUrl);
			request.getSession().setAttribute("redirectUrl", redirectUrl);
			WebUtils.removeCookie(request, response, REDIRECT_TOKEN_COOKIE_NAME);
		}
		if (socialUserId != null && StringUtils.isNotEmpty(uniqueId)) {
			SocialUser socialUser = socialUserService.find(socialUserId);
			if (socialUser == null || socialUser.getUser() != null || !StringUtils.equals(socialUser.getUniqueId(), uniqueId)) {
				return UNPROCESSABLE_ENTITY_VIEW;
			}
			model.addAttribute("socialUserId", socialUserId);
			model.addAttribute("uniqueId", uniqueId);
		}
		model.addAttribute("loginPlugins", pluginService.getActiveLoginPlugins(request));
		
		
		//跳过登录
		String userCode = request.getParameter("userCode");
		if(userCode != null){
			String signature = request.getParameter("signature");
			String timestamp = request.getParameter("timestamp").toString();
			System.out.println();
			String appointtrue = DigestUtils.md5Hex(timestamp+urlSignature);
			System.out.println("MD5:"+appointtrue);
			System.out.println("接口传过来的时间戳："+timestamp+"当前时间戳"+System.currentTimeMillis() / 1000);
			//时间差
			Long timeT = TimeUtil.validateTimeStamp(Long.parseLong(timestamp));
			System.out.println(timeT);
			if(timeT < 3 && appointtrue.equals(signature)){
				try {
					userService.login(new UserAuthenticationToken(Member.class,userCode , "a123456", false, request.getRemoteAddr()));					
				} catch (Exception e) {
					System.out.println("数据库没有"+userCode+"这个会员编号");
				}
			}else if(!(timeT < 3) && appointtrue.equals(signature)){
				userService.logout();
			}
			return "redirect:/";
		}else{
			return currentUser != null ? "redirect:" + memberIndex : memberLoginView;
		}
		
	}
}