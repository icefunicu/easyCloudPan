Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
[WARNING]
[WARNING] Some problems were encountered while building the effective settings
[WARNING] Unrecognised tag: 'repository' (position: START_TAG seen ...<profile>\n\t\t<repository>... @220:15)  @ C:\language_pakge\apache-maven-3.9.2\conf\settings.xml, line 220, column 15
[WARNING]
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------< com.easypan:easypan >-------------------------
[INFO] Building easypan 1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] >>> spring-boot:3.2.12:run (default-cli) > test-compile @ easypan >>>
[INFO]
[INFO] --- checkstyle:3.3.1:check (validate) @ easypan ---
[INFO] 开始检查……
[WARN] E:\Project\easyCloudPan\backend\src\main\java\com\easypan\utils\QueryWrapperBuilder.java:36:5: 缺少 Javadoc 。 [MissingJavadocMethod]
[WARN] E:\Project\easyCloudPan\backend\src\main\java\com\easypan\utils\QueryWrapperBuilder.java:129:5: 缺少 Javadoc 。 [MissingJavadocMethod]
[WARN] E:\Project\easyCloudPan\backend\src\main\java\com\easypan\utils\QueryWrapperBuilder.java:176:5: 缺少 Javadoc 。 [MissingJavadocMethod]
检查完成。
[INFO] You have 0 Checkstyle violations.
[INFO]
[INFO] --- jacoco:0.8.11:prepare-agent (default) @ easypan ---
[INFO] argLine set to -javaagent:E:\\language_pakge\\apache-maven-3.9.2\\mvn_repo\\org\\jacoco\\org.jacoco.agent\\0.8.11\\org.jacoco.agent-0.8.11-runtime.jar=destfile=E:\\Project\\easyCloudPan\\backend\\target\\jacoco.exec
[INFO]
[INFO] --- build-helper:3.5.0:add-source (add-source) @ easypan ---
[INFO] Source directory: E:\Project\easyCloudPan\backend\target\generated-sources\annotations added.
[INFO]
[INFO] --- resources:3.3.1:resources (default-resources) @ easypan ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 13 resources from src\main\resources to target\classes
[INFO]
[INFO] --- compiler:3.11.0:compile (default-compile) @ easypan ---
[INFO] Nothing to compile - all classes are up to date
[INFO]
[INFO] --- resources:3.3.1:testResources (default-testResources) @ easypan ---
[INFO] Copying 2 resources from src\test\resources to target\test-classes
[INFO]
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ easypan ---
[INFO] Nothing to compile - all classes are up to date
[INFO]
[INFO] <<< spring-boot:3.2.12:run (default-cli) < test-compile @ easypan <<<
[INFO]
[INFO]
[INFO] --- spring-boot:3.2.12:run (default-cli) @ easypan ---
[INFO] Attaching agents: []
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.12)

2026-02-13 21:02:45.253 [INFO ] [c.u.j.EncryptablePropertySourceConverter] [main] - Skipping PropertySource servletConfigInitParams [class org.springframework.core.env.PropertySource$StubPropertySource
  __  __       _           _   _       _____ _
 |  \/  |_   _| |__   __ _| |_(_)___  |  ___| | _____  __
 | |\/| | | | | '_ \ / _` | __| / __| | |_  | |/ _ \ \/ /
 | |  | | |_| | |_) | (_| | |_| \__ \ |  _| | |  __/>  <
 |_|  |_|\__, |_.__/ \__,_|\__|_|___/ |_|   |_|\___/_/\_\
         |___/ v1.11.6 https://mybatis-flex.com
2026-02-13 21:02:46.751 [INFO ] [com.zaxxer.hikari.pool.HikariPool] [main] - EasyPanHikariCP - Added connection org.postgresql.jdbc.PgConnection@3c2f310c
2026-02-13 21:02:48.656 [INFO ] [o.s.s.web.DefaultSecurityFilterChain] [main] - Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@4377b35b, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@20dc2df1, org.springframework.security.web.context.SecurityContextHolderFilter@222af5ff, org.springframework.security.web.header.HeaderWriterFilter@675c1335, org.springframework.security.web.authentication.logout.LogoutFilter@67bf393c, com.easypan.component.JwtAuthenticationFilter@1256925b, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@17478cec, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@7d593bbc, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@3a5922ec, org.springframework.security.web.access.ExceptionTranslationFilter@17f4276b, org.springframework.security.web.access.intercept.AuthorizationFilter@1ca6323c]
2026-02-13 21:02:49.389 [INFO ] [o.a.coyote.http11.Http11NioProtocol] [main] - Starting ProtocolHandler ["http-nio-7090"]
2026-02-13 21:02:49.400 [INFO ] [c.u.j.c.CachingDelegateEncryptablePropertySource] [main] - Property Source commandLineArgs refreshed
2026-02-13 21:02:49.466 [INFO ] [com.easypan.InitRun] [main] - HikariProxyConnection@1629265201 wrapping org.postgresql.jdbc.PgConnection@3c2f310c
DEBUG: Jakarta Mail version 2.1.3
DEBUG: URL jar:file:/E:/language_pakge/apache-maven-3.9.2/mvn_repo/org/eclipse/angus/jakarta.mail/2.0.3/jakarta.mail-2.0.3.jar!/META-INF/javamail.providers
DEBUG: successfully loaded resource: jar:file:/E:/language_pakge/apache-maven-3.9.2/mvn_repo/org/eclipse/angus/jakarta.mail/2.0.3/jakarta.mail-2.0.3.jar!/META-INF/javamail.providers
DEBUG: successfully loaded resource: /META-INF/javamail.default.providers
DEBUG: Tables of loaded providers
DEBUG: Providers Listed By Class Name: {org.eclipse.angus.mail.imap.IMAPStore=jakarta.mail.Provider[STORE,imap,org.eclipse.angus.mail.imap.IMAPStore,Oracle], org.eclipse.angus.mail.smtp.SMTPTransport=jakarta.mail.Provider[TRANSPORT,smtp,org.eclipse.angus.mail.smtp.SMTPTransport,Oracle], org.eclipse.angus.mail.pop3.POP3Store=jakarta.mail.Provider[STORE,pop3,org.eclipse.angus.mail.pop3.POP3Store,Oracle], org.eclipse.angus.mail.pop3.POP3SSLStore=jakarta.mail.Provider[STORE,pop3s,org.eclipse.angus.mail.pop3.POP3SSLStore,Oracle], org.eclipse.angus.mail.smtp.SMTPSSLTransport=jakarta.mail.Provider[TRANSPORT,smtps,org.eclipse.angus.mail.smtp.SMTPSSLTransport,Oracle], org.eclipse.angus.mail.imap.IMAPSSLStore=jakarta.mail.Provider[STORE,imaps,org.eclipse.angus.mail.imap.IMAPSSLStore,Oracle]}
DEBUG: Providers Listed By Protocol: {imap=jakarta.mail.Provider[STORE,imap,org.eclipse.angus.mail.imap.IMAPStore,Oracle], smtp=jakarta.mail.Provider[TRANSPORT,smtp,org.eclipse.angus.mail.smtp.SMTPTransport,Oracle], pop3=jakarta.mail.Provider[STORE,pop3,org.eclipse.angus.mail.pop3.POP3Store,Oracle], imaps=jakarta.mail.Provider[STORE,imaps,org.eclipse.angus.mail.imap.IMAPSSLStore,Oracle], smtps=jakarta.mail.Provider[TRANSPORT,smtps,org.eclipse.angus.mail.smtp.SMTPSSLTransport,Oracle], pop3s=jakarta.mail.Provider[STORE,pop3s,org.eclipse.angus.mail.pop3.POP3SSLStore,Oracle]}
DEBUG: successfully loaded resource: /META-INF/javamail.default.address.map
DEBUG: URL jar:file:/E:/language_pakge/apache-maven-3.9.2/mvn_repo/org/eclipse/angus/jakarta.mail/2.0.3/jakarta.mail-2.0.3.jar!/META-INF/javamail.address.map
DEBUG: successfully loaded resource: jar:file:/E:/language_pakge/apache-maven-3.9.2/mvn_repo/org/eclipse/angus/jakarta.mail/2.0.3/jakarta.mail-2.0.3.jar!/META-INF/javamail.address.map
DEBUG: getProvider() returning jakarta.mail.Provider[TRANSPORT,smtp,org.eclipse.angus.mail.smtp.SMTPTransport,Oracle]
DEBUG SMTP: useEhlo true, useAuth false
DEBUG SMTP: trying to connect to host "smtp.qq.com", port 465, isSSL false
220 newxmesmtplogicsvrszb51-1.qq.com XMail Esmtp QQ Mail Server.
DEBUG SMTP: connected to host "smtp.qq.com", port: 465
EHLO PAelen
250-newxmesmtplogicsvrszb51-1.qq.com
250-PIPELINING
250-SIZE 73400320
250-AUTH LOGIN PLAIN XOAUTH XOAUTH2
250-AUTH=LOGIN
250-MAILCOMPRESS
250-SMTPUTF8
250 8BITMIME
DEBUG SMTP: Found extension "PIPELINING", arg ""
DEBUG SMTP: Found extension "SIZE", arg "73400320"
DEBUG SMTP: Found extension "AUTH", arg "LOGIN PLAIN XOAUTH XOAUTH2"
DEBUG SMTP: Found extension "AUTH=LOGIN", arg ""
DEBUG SMTP: Found extension "MAILCOMPRESS", arg ""
DEBUG SMTP: Found extension "SMTPUTF8", arg ""
DEBUG SMTP: Found extension "8BITMIME", arg ""
DEBUG SMTP: protocolConnect login, host=smtp.qq.com, user=2041487752@qq.com, password=<non-null>
DEBUG SMTP: Attempt to authenticate using mechanisms: LOGIN PLAIN DIGEST-MD5 NTLM XOAUTH2
DEBUG SMTP: Using mechanism LOGIN
DEBUG SMTP: AUTH LOGIN command trace suppressed
DEBUG SMTP: AUTH LOGIN succeeded
QUIT
221 Bye.
2026-02-13 21:03:16.776 [ERROR] [c.e.controller.AccountController] [tomcat-handler-34] - 上传头像失败
java.lang.NullPointerException: Cannot invoke "org.springframework.web.multipart.MultipartFile.transferTo(java.io.File)" because "avatar" is null
        at com.easypan.controller.AccountController.updateUserAvatar(AccountController.java:378)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:355)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint.proceed(MethodInvocationProceedingJoinPoint.java:89)
        at com.easypan.aspect.PerformanceMonitorAspect.monitorPerformance(PerformanceMonitorAspect.java:38)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethodWithGivenArgs(AbstractAspectJAdvice.java:637)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethod(AbstractAspectJAdvice.java:627)
        at org.springframework.aop.aspectj.AspectJAroundAdvice.invoke(AspectJAroundAdvice.java:71)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor.invoke(MethodBeforeAdviceInterceptor.java:58)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:173)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:97)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:720)
        at com.easypan.controller.AccountController$$SpringCGLIB$$0.updateUserAvatar(<generated>)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:255)
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:188)
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:926)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:831)
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979)
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014)
        at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:914)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:590)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:195)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:110)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108)
        at org.springframework.security.web.FilterChainProxy.lambda$doFilterInternal$3(FilterChainProxy.java:231)
        at org.springframework.security.web.ObservationFilterChainDecorator$FilterObservation$SimpleFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:479)
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:340)
        at org.springframework.security.web.ObservationFilterChainDecorator.lambda$wrapSecured$0(ObservationFilterChainDecorator.java:82)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:128)
        at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(AuthorizationFilter.java:100)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:126)
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:120)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter(AnonymousAuthenticationFilter.java:100)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter(SecurityContextHolderAwareRequestFilter.java:179)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter(RequestCacheAwareFilter.java:63)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at com.easypan.component.JwtAuthenticationFilter.doFilterInternal(JwtAuthenticationFilter.java:78)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:107)
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:93)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90)
        at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:82)
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:69)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:62)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.session.DisableEncodeUrlFilter.doFilterInternal(DisableEncodeUrlFilter.java:42)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$0(ObservationFilterChainDecorator.java:323)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:224)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:233)
        at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:191)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113)
        at org.springframework.web.servlet.handler.HandlerMappingIntrospector.lambda$createCacheFilter$3(HandlerMappingIntrospector.java:195)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113)
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74)
        at org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebMvcSecurityConfiguration.java:230)
        at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:362)
        at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:278)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.ServerHttpObservationFilter.doFilterInternal(ServerHttpObservationFilter.java:113)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:483)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:115)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:344)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:397)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:905)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1741)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52)
        at java.base/java.lang.VirtualThread.run(VirtualThread.java:309)