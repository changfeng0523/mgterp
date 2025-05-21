package com.mogutou.erp.controller;

import com.mogutou.erp.common.Result;
import com.mogutou.erp.config.JwtConfig;
import com.mogutou.erp.entity.User;
import com.mogutou.erp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        // 从请求中获取用户名和密码
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        // 验证请求参数
        if (username == null || password == null) {
            return Result.error(400, "用户名和密码不能为空");
        }
        
        // 查找用户
        Optional<User> userOpt = userService.findByUsername(username);
        
        // 验证用户名和密码
        if (userOpt.isPresent() && userService.verifyPassword(password, userOpt.get().getPassword())) {
            User user = userOpt.get();
            
            // 更新最后登录时间
            userService.updateLoginTime(user);
            
            // 生成JWT令牌
            String token = jwtConfig.generateToken(user.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("token", "Bearer " + token);
            return Result.success("登录成功", response);
        } else {
            return Result.error(401, "用户名或密码错误");
        }
    }
    
    @GetMapping("/user")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username != null) {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("name", user.getUsername());
                userInfo.put("roles", new String[]{user.getRole()});
                userInfo.put("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
                userInfo.put("tel", user.getTel());
                userInfo.put("email", user.getEmail());
                return Result.success(userInfo);
            }
        }
        return Result.error(401, "未授权");
    }
    
    @GetMapping("/logout")
    public Result<Void> logout() {
        // 在实际应用中，可能需要处理令牌失效等逻辑
        // 这里简单返回成功响应
        return Result.success("注销成功");
    }
    
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String password = registerRequest.get("password");
        String tel = registerRequest.get("tel");
        String email = registerRequest.get("email");
        
        // 验证必要字段不为空
        if (username == null || password == null || tel == null) {
            return Result.error(400, "用户名、密码和电话号码不能为空");
        }
        
        // 检查用户名是否已存在
        if (userService.isUsernameExists(username)) {
            return Result.error(400, "用户名已存在");
        }
        
        // 检查电话号码是否已存在
        if (userService.isTelExists(tel)) {
            return Result.error(400, "电话号码已被注册");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // UserService会处理密码加密
        user.setTel(tel);
        user.setEmail(email);
        
        // 保存用户
        userService.createUser(user);
        
        return Result.success("注册成功");
    }
}