/**
 * 
 */

package com.xyz.mybatis.core.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mybatis.core.annotation.Repository;
import com.xyz.mybatis.core.scanner.DefaultClassScanner;

/**
 * @author lee.
 */
public class SessionFactoryBean {
  private static final Logger logger = LoggerFactory.getLogger(SessionFactoryBean.class);
  private static SqlSessionFactory sqlSessionFactory;
  private static final String resource = "mybatis.xml";
  private static final String basepackage = "com.xyz.mybatis.demo.dao";
  private static final ConcurrentHashMap<Class<?>, Object> repositories = new ConcurrentHashMap<>();
  private static final SessionFactoryBean intance = new SessionFactoryBean();

  private static ThreadLocal<ConnectionHolder> sessionLocal;

  public SessionFactoryBean() {
    getSqlSessionFactory();
    sessionLocal = new ThreadLocal<ConnectionHolder>();
  }

  public SqlSessionFactory getSqlSessionFactory() {
    if (sqlSessionFactory == null) {
      synchronized (SessionFactoryBean.class) {
        if (sqlSessionFactory == null) {
          initSqlSessionFactory();
        }
      }
    }
    return sqlSessionFactory;
  }

  private void initSqlSessionFactory() {
    try {
      InputStream inputStream = Resources.getResourceAsStream(resource);
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
      scan(basepackage);
    } catch (IOException err) {
      logger.error("initSqlSessionFactory error ", err);
    }
  }

  /**
   * @param basepackage
   */
  private void scan(String basepackage) {
    try {
      Set<Class<?>> classes = DefaultClassScanner.getInstance().getClassListByAnnotation(basepackage, Repository.class);
      for (Class<?> item : classes) {
        logger.info("Repository DAO : " + item);
        SqlSessionProxy<?> dao = new SqlSessionProxy<>(getSqlSessionFactory(), item);
        repositories.putIfAbsent(item, dao.getObject());
      }
    } catch (Exception err) {
      logger.error("scan package error ", err);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getMapper(Class<T> type) {
    return (T) repositories.get(type);
  }

  public static ThreadLocal<ConnectionHolder> getSessionLocal() {
    return getSessionLocal(true);
  }

  public static ThreadLocal<ConnectionHolder> getSessionLocal(boolean autoCommit) {
    if (null == sessionLocal.get()) {
      sessionLocal.set(new ConnectionHolder(getInstance().getSqlSessionFactory().openSession(autoCommit), autoCommit));
    } else if (sessionLocal.get().getSession() == null) {
      sessionLocal.get().setSession(getInstance().getSqlSessionFactory().openSession(autoCommit));
    }
    return sessionLocal;
  }

  public static SessionFactoryBean getInstance() {
    return intance;
  }

}
