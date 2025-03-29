package com.neel.security_no_gateway_testing.async;

import com.neel.security_no_gateway_testing.usercontext.UserContext;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import org.springframework.core.task.TaskDecorator;

class ContextCopyingDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the current thread's context
        //RequestAttributes context = RequestContextHolder.currentRequestAttributes();
        //Map<String, String> contextMap = MDC.getCopyOfContextMap();
        UserContext userContext = UserContextHolder.getUserContext();

        return () -> {
            try {
                // Set the context on the new thread
                /*RequestContextHolder.setRequestAttributes(context);
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }*/
                if (userContext != null) {
                    UserContextHolder.setUserContext(userContext);
                }

                // Execute the actual task
                runnable.run();
            } finally {
                // Clean up the context
                //RequestContextHolder.resetRequestAttributes();
                //MDC.clear();
                UserContextHolder.clear();
            }
        };
    }
}
