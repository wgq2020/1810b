package com.jk.wgq.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.jk.wgq.bean.User;
import com.jk.wgq.mapper.LoginMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService{

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private JedisPool jedisPool;

    @Override
    public HashMap<String, Object> login(User user) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (StringUtils.isNotEmpty(user.getUser_name()) && StringUtils.isNotEmpty(user.getUser_password())){
            User use = loginMapper.login(user.getUser_name(), user.getUser_password());
            if (use != null){
                hashMap.put("msg", "登陆成功");
                hashMap.put("user",use);
                hashMap.put("status","200");
            }else{
                hashMap.put("msg","用户名或者密码不正确");
                hashMap.put("status","404");
            }
        }else{
            hashMap.put("msg","用户名密码不能为空");
            hashMap.put("status","400");
        }

        return hashMap;
    }

    @Override
    public HashMap<String, Object> registerUser(User user) {

        Jedis redis = jedisPool.getResource();

        HashMap<String, Object> hashMap = new HashMap<>();
        if (StringUtils.isNotEmpty(user.getUser_name()) && StringUtils.isNotEmpty(user.getUser_password()) && StringUtils.isNotEmpty(user.getUserEmailCode()) && StringUtils.isNotEmpty(user.getUser_email())){

            String email = redis.get(user.getUser_email());
            String emailCode = redis.get(user.getUser_email() + "Code");
            if (email.equals(user.getUser_email()) && emailCode.equals(user.getUserEmailCode()) && StringUtils.isNotEmpty(email) && StringUtils.isNotEmpty(emailCode)){

                List<User> users = loginMapper.findUserByUserName(user.getUser_name());

                List<User> use = loginMapper.findUserByUserEmail(user.getUser_email());

                if (users.size() > 0){
                    hashMap.put("status", "error");
                    hashMap.put("reason", "该用户名已经有人注册");
                }else if (use.size() > 0){
                    hashMap.put("status", "error");
                    hashMap.put("reason", "该邮箱已经有人注册");
                }else{
                    user.setUser_createtime(new Date());
                    user.setUser_id(UUID.randomUUID().toString());
                        loginMapper.addUser(user);

                    hashMap.put("reason", "注册成功");
                    hashMap.put("status", "200");
                }
            }else{

                if (emailCode.equals(user.getUserEmailCode()) && emailCode != null){
                    hashMap.put("status", "error");
                    hashMap.put("reason", "验证码不正确");
                }else if (StringUtils.isEmpty(emailCode)){
                    hashMap.put("status", "error");
                    hashMap.put("reason", "验证码已过期，请再次接收验证码");
                }
            }


        }else{
            hashMap.put("status", "400");
            hashMap.put("reason", "还有未填写数据");
        }

        redis.close();
        return hashMap;
    }

    @Override
    public HashMap<String, Object> registerEmailCode(String email) {
        HashMap<String, Object> hashMap = new HashMap<>();
        Jedis redis = jedisPool.getResource();;//连接池
        HtmlEmail htmlEmail;//创建一个htmlEmail实例对象
        List<User> use = null;//存放查询出来的用户数据
        int code;//验证码

        if (StringUtils.isNotEmpty(email) && email != ""){
            use = loginMapper.findUserByUserEmail(email);//查询是否已经有人注册过该邮箱
        }

        if (use.size() > 0 && use != null){
            hashMap.put("status", "4");
            hashMap.put("msg", "该邮箱已经有人注册");
        }else {

            if (email.matches("^\\w+@(\\w+\\.)+\\w+$") && StringUtils.isNotEmpty(email)) {
                code = (int) ((Math.random() * 9 + 1) * 100000);
                htmlEmail = new HtmlEmail();

                try {
                    htmlEmail.setHostName("smtp.qq.com");//设置邮箱的SMTP服务器，登录相对应的邮箱官网，去拿就行了,邮箱的SMTP服务器，一般163邮箱的是smtp.163.com,qq邮箱为smtp.qq.com
                    htmlEmail.setCharset("utf-8");//设置发送的字符类型
                    htmlEmail.setFrom("1583365095@qq.com", "金科教育");//设置发送人的邮箱和用户名，用户名可以自行定义
                    htmlEmail.setAuthentication("1583365095@qq.com", "ngotnvgiyxoegdia");//设置发送人到的邮箱和用户名和授权码(授权码是自己设置的)
                    htmlEmail.setSubject("北京金科教育");//设置发送主题
                    htmlEmail.setMsg("您的验证码为：" + code + "，验证码将在1分钟后过期，请尽快输入验证码完成注册！");//设置发送内容

                    htmlEmail.addTo(email);//设置收件人
                    htmlEmail.send();//进行发送

                    redis.set(email + "Code", "" + code);
                    redis.set(email, email);
                    redis.expire(email, 60);
                    redis.expire(email + "Code", 60);
                    //hashMap.put("code", code);
                    hashMap.put("email", email);
                    hashMap.put("status", "0");

                    System.out.println("邮件发送成功");
                    System.out.println(htmlEmail.getSmtpPort());
                } catch (Exception e) {
                    System.out.println("邮件发送失败");
                    e.printStackTrace();
                }

            } else {

                if (StringUtils.isEmpty(email)){
                    hashMap.put("msg", "不能为空");
                }else{
                    hashMap.put("msg", "只支持邮箱格式");
                }
            }
        }
        redis.close();
        return hashMap;
    }
}
