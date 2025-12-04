package app.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class ActionLoggingAspect {

    // log for execute controller method
    @Before("execution(* app.controller..*(..))")
    public void logBefore(JoinPoint jp) {
        String username = getUsername();
        System.out.println("➡️ BEFORE: User [" + username + "] calls: " +
                jp.getSignature().toShortString() +
                " with args: " + Arrays.toString(jp.getArgs()));
    }

    // log for response
    @AfterReturning(pointcut = "execution(* app.controller..*(..))", returning = "result")
    public void logAfter(JoinPoint jp, Object result) {
        String username = getUsername();
        System.out.println("✔️ AFTER RETURN: User [" + username + "] finished: " +
                jp.getSignature().toShortString() +
                " returned: " + result);
    }

    // 3) log for errors in controllers
    @AfterThrowing(pointcut = "execution(* app.controller..*(..))", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        String username = getUsername();
        System.out.println("❌ ERROR: User [" + username + "] caused exception in: "
                + jp.getSignature().toShortString() +
                " -> " + ex.getMessage());
    }

    // log for performance of service methods (measure execution time)
    @Around("execution(* app.service..*(..))")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String username = getUsername();

        Object returned;

        try {
            returned = pjp.proceed();
        } catch (Throwable ex) {
            System.out.println("🔥 SERVICE ERROR by [" + username + "] → " +
                    pjp.getSignature().toShortString() +
                    " | Message: " + ex.getMessage());
            throw ex;
        }

        long end = System.currentTimeMillis();
        long diff = end - start;

        System.out.println("⏱️ PERFORMANCE: [" + username + "] executed " +
                pjp.getSignature().toShortString() +
                " in " + diff + "ms");

        return returned;
    }

    // log for logged user
    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? "ANONYMOUS" : auth.getName();
    }
}
