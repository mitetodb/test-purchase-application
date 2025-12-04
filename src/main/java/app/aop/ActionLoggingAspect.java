package app.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ActionLoggingAspect {

    // log for execute controller method
    @Before("execution(* app.controller..*(..))")
    public void logBefore(JoinPoint jp) {
        String username = getUsername();
        log.info("➡️ BEFORE: User [{}] calls: {} with args: {}",
                username,
                jp.getSignature().toShortString(),
                Arrays.toString(jp.getArgs()));
    }

    // log for response
    @AfterReturning(pointcut = "execution(* app.controller..*(..))", returning = "result")
    public void logAfter(JoinPoint jp, Object result) {
        String username = getUsername();
        log.info("✔️ AFTER RETURN: User [{}] finished: {} returned: {}",
                username,
                jp.getSignature().toShortString(),
                result);
    }

    // log for errors in controllers
    @AfterThrowing(pointcut = "execution(* app.controller..*(..))", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        String username = getUsername(); // твоя метод
        log.error("❌ Controller error: user [{}], method [{}], message: {}",
                username,
                jp.getSignature().toShortString(),
                ex.getMessage(),
                ex);
    }

    // log for performance of service methods (measure execution time)
    @Around("execution(* app.service..*(..))")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String username = getUsername();

        try {
            Object returned = pjp.proceed();
            long diff = System.currentTimeMillis() - start;

            log.info("⏱️ PERFORMANCE: [{}] executed {} in {} ms",
                    username,
                    pjp.getSignature().toShortString(),
                    diff);

            return returned;
        } catch (Throwable ex) {
            log.error("🔥 SERVICE ERROR by [{}] in {} | Message: {}",
                    username,
                    pjp.getSignature().toShortString(),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    // log for logged user
    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? "ANONYMOUS" : auth.getName();
    }
}
