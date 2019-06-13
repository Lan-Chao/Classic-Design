/*
 *  定义切面（spring boot配置方式）
 *
 *  在实际的使用过程中，分布式锁可以封装好后使用在方法级别，这样就不用每个地方都去获取锁和释放锁，使用起来更加方便。
 *
 *  spring boot starter还需要在 resources/META-INF 中添加 spring.factories 文件
 *  # Auto Configure
 *  org.springframework.boot.autoconfigure.EnableAutoConfiguration=
 *  com.itopener.lock.redis.spring.boot.autoconfigure.DistributedLockAutoConfiguration,
 *  com.itopener.lock.redis.spring.boot.autoconfigure.DistributedLockAspectConfiguration
 */


import java.lang.reflect.Method;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import com.itopener.lock.redis.spring.boot.autoconfigure.annotations.RedisLock;
import com.itopener.lock.redis.spring.boot.autoconfigure.annotations.RedisLock.LockFailAction;
import com.itopener.lock.redis.spring.boot.autoconfigure.lock.DistributedLock;



@Aspect
@Configuration
@ConditionalOnClass(DistributedLock.class)
@AutoConfigureAfter(DistributedLockAutoConfiguration.class)
public class DistributedLockAspectConfiguration {
     private final Logger logger = LoggerFactory.getLogger(DistributedLockAspectConfiguration.class);
     @Autowired
     private DistributedLock distributedLock;
     
     @Pointcut("@annotation(com.itopener.lock.redis.spring.boot.autoconfigure.annotations.RedisLock)")
        private void lockPoint(){
     }
     
     @Around("lockPoint()")
     public Object around(ProceedingJoinPoint pjp) throws Throwable{
         Method method = ((MethodSignature) pjp.getSignature()).getMethod();
         RedisLock redisLock = method.getAnnotation(RedisLock.class);
         String key = redisLock.value();
         if(StringUtils.isEmpty(key)){
             Object[] args = pjp.getArgs();
             key = Arrays.toString(args);
         }
         int retryTimes = redisLock.action().equals(LockFailAction.CONTINUE) ? redisLock.retryTimes() : 0;
         boolean lock = distributedLock.lock(key, redisLock.keepMills(), retryTimes, redisLock.sleepMills());
         if(!lock) {
            logger.debug("get lock failed : " + key);
            return null;
         }
     
         //得到锁,执行方法，释放锁
         logger.debug("get lock success : " + key);
         try {
            return pjp.proceed();
         } catch (Exception e) {
            logger.error("execute locked method occured an exception", e);
         } finally {
            boolean releaseResult = distributedLock.releaseLock(key);
            logger.debug("release lock : " + key + (releaseResult ? " success" : " failed"));
         }
        return null;
     }
}
