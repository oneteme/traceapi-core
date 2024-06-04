package org.usf.traceapi.core;

import static java.time.Instant.now;
import static org.usf.traceapi.core.Helper.log;

import java.time.Instant;
import java.util.function.Consumer;

import org.usf.traceapi.core.SafeCallable.SafeRunnable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StageTracker {

	public static <E extends Throwable> void exec(SafeRunnable<E> fn, StageConsumer<? super Void> cons) throws E {
		call(fn, cons);
	}

	public static <E extends Throwable, S> void exec(SafeRunnable<E> fn, StageCreator<? super Void, S> creator, Consumer<S> cons) throws E {
		call(fn, creator.andThen(cons));
	}

	public static <T, E extends Throwable, S> T call(SafeCallable<T,E> fn, StageCreator<? super T, S> creator, Consumer<S> cons) throws E {
		return call(fn, creator.andThen(cons));
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, StageConsumer<? super T> cons) throws E {
		T o = null;
		Throwable t = null;
		var s = now();
		try {
			return (o = fn.call());
		}
		catch(Throwable e) { //also error
			t  = e;
			throw e;
		}
		finally {
			var e = now();
			try {
				cons.accept(s, e, o, t);
			}
			catch (Exception ex) {
				//do not throw exception
				log.warn("cannot collect stage metrics, {}", ex.getMessage());
			}
		}
	}
	
	@FunctionalInterface
	public static interface StageConsumer<T> {
		
		void accept(Instant start, Instant end, T o, Throwable t) throws Exception;
	}
	

	public static interface StageCreator<T, S> {

		S create(Instant start, Instant end, T o, Throwable t) throws Exception;
		
		default StageConsumer<T> andThen(Consumer<S> cons){
			return (s,e,o,t)-> cons.accept(create(s, e, o, t));
		}
	}
}
