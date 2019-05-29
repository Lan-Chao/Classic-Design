/**
 * ErrorContext的单例实现代码
 *
 * LOCAL的静态实例变量使用了ThreadLocal修饰，也就是说它属于每个线程各自的数据，
 * 而在instance()方法中，先获取本线程的该实例，如果没有就创建该线程独有的ErrorContext。
 */

public class ErrorContext {

	private static final ThreadLocal<ErrorContext> LOCAL = new ThreadLocal<ErrorContext>();

	private ErrorContext() {
	}

	public static ErrorContext instance() {
		ErrorContext context = LOCAL.get();
		if (context == null) {
			context = new ErrorContext();
			LOCAL.set(context);
		}
		return context;
	}
  
}
