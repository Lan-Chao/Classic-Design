

注解：@ModelAttribute
//一开始进入Controller执行该方法
@ModelAttribute
public void getUser(@RequestParam(value="userName",required=false) String userName,Model model){

    User user = new User(userName,"123456");
    model.addAttribute("user", user);
}
@ModelAttribute注解getUser方法,getUser方法接收前台提交的userName数据,在model中放入user属性和数据。
 
//真正的请求url
@RequestMapping("/testModelAttribute")
public String testModelAttribute(ModelMap model){
    System.out.println("testModelAttribute user:"+model.get("user"));
    return "success";
}

@ModelAttribute
public User getUser(@RequestParam(value="userName",required=false) String userName){
 
    User user = new User(userName,"123456");
    return user;
}
这种情况隐含的将返回的数据放入model中，等同于model.addAttribute("user", user); 
我们可以设置@ModelAttribute的value属性来执行model中数据的key值，@ModelAttribute("user")








