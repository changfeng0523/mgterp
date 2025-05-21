package com.mogutou.erp.config;

import com.mogutou.erp.entity.User;
import com.mogutou.erp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;


@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private UserService userService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtConfig.validateToken(token)) {
                String username = jwtConfig.getUsernameFromToken(token);
                // 验证用户是否存在于数据库中
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent() && userOpt.get().getStatus()) {
                    request.setAttribute("username", username);
                    request.setAttribute("userId", userOpt.get().getId());
                    return true;
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}