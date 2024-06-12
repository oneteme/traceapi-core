package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.updateThreadLocalSession;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.Session.sessionStageAppender;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.core.StageTracker.exec;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	private final ExecutorService es;
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return aroundCallable(task, es::submit);
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		return aroundRunnable(task, es::submit);
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return aroundRunnable(task, c-> es.submit(c, result));
	}
	
	@Override
	public void execute(Runnable command) {
		aroundRunnable(command, c-> {es.execute(c); return null;});
	}
	
    private static <T> T aroundRunnable(Runnable command, Function<Runnable, T> fn) {
    	var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return fn.apply(command);
		}
		session.lock(); //important! sync lock
		try {
			var app = sessionStageAppender(session); //important! run on parent thread
			return fn.apply(()->{
				updateThreadLocalSession(session); //thread already exists
		    	try {
			    	exec(command::run, app);
		    	}
		    	finally {
					session.unlock();
					localTrace.remove(); 
		    	}
			});
		}
		catch (Exception e) {  //@see Executor::execute
			session.unlock();
			throw e;
		}
    }

    private static <T,V> V aroundCallable(Callable<T> command, Function<Callable<T>, V> fn) {
    	var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return fn.apply(command);
		}
		session.lock(); //important! sync lock
		try {
			var app = sessionStageAppender(session); //important! run on parent thread
			return fn.apply(()->{
				updateThreadLocalSession(session); //thread already exists
		    	try {
		    		return call(command::call, app);
		    	}
		    	finally {
					session.unlock();
					localTrace.remove(); 
		    	}
			});
		}
		catch (Exception e) {  //@see Executor::execute
			session.unlock();
			throw e;
		}
    }
        
	public static ExecutorServiceWrapper wrap(@NonNull ExecutorService es) {
		return new ExecutorServiceWrapper(es);
	}
}