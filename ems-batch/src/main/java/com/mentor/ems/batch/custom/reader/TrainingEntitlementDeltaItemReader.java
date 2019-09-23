/**
 * 
 */
package com.mentor.ems.batch.custom.reader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.mentor.ems.common.entity.OrderDetail;

/**
 * @author sbhandar
 * @param <T>
 *            Custom JPAReader to fetch the record always from 0th index
 *
 */
public class TrainingEntitlementDeltaItemReader<T> extends AbstractPagingItemReader<T> {

	private EntityManagerFactory entityManagerFactory;

	private EntityManager entityManager;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();

	private String queryString;

	private JpaQueryProvider queryProvider;

	private Map<String, Object> parameterValues;

	private boolean transacted = true;// default value

	private static final Logger LOGGER = LoggerFactory.getLogger(TrainingEntitlementDeltaItemReader.class);

	public TrainingEntitlementDeltaItemReader() {
		setName(ClassUtils.getShortName(TrainingEntitlementDeltaItemReader.class));
	}

    private static Long trainingEntitlementKey = null;
    private static Long orderDetailStartKey = null;

	/**
	 * Create a query using an appropriate query provider (entityManager OR
	 * queryProvider).
	 */
	private Query createQuery() {
		if (queryProvider == null) {
			LOGGER.debug("Query String " + queryString);

			if (null == trainingEntitlementKey) {
				Query queryStr = entityManager
						.createQuery("select max(trainingEntitlementKey) from TrainingEntitlement");
				trainingEntitlementKey = (Long) queryStr.getSingleResult();
				LOGGER.error("Maximum value of trainingEntitlementKey is:" + trainingEntitlementKey);
			}

			LOGGER.debug("orderDetailStartKey value is:" + orderDetailStartKey);

			if (orderDetailStartKey == null) {
				orderDetailStartKey = new Long(0);
			}
			return entityManager.createQuery("select od from OrderDetail od "
					+ "join OrderHeader oh on od.orderHeader.orderHdrKey=oh.orderHdrKey and (trunc(od.contractStartDt) "
					+ "<=trunc(sysdate) or trunc(od.contractEndDt) <=trunc(sysdate)) JOIN "
					+ "TrainingEntitlement ece on od.orderDetailKey =ece.orderDetail.orderDetailKey "
					+ "JOIN Codes ittyp on ece.itemType.codeKey=ittyp.codeKey and ittyp.codeValue in "
					+ "('Training Backlog','Training Active') JOIN Codes sts on ece.statusCd.codeKey=sts.codeKey and "
					+ "sts.codeValue ='ACTIVE' and ece.trainingEntitlementKey<:trainingEntitlementKey where od.orderDetailKey>:orderDetailtStartKey order by od.orderDetailKey")
					.setParameter("trainingEntitlementKey", trainingEntitlementKey)
					.setParameter("orderDetailtStartKey", orderDetailStartKey);
		} else {
			return queryProvider.createQuery();
		}
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * The parameter values to be used for the query execution.
	 *
	 * @param parameterValues
	 *            the values keyed by the parameter named used in the query string.
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * By default (true) the EntityTransaction will be started and committed around
	 * the read. Can be overridden (false) in cases where the JPA implementation
	 * doesn't support a particular transaction. (e.g. Hibernate with a JTA
	 * transaction). NOTE: may cause problems in guaranteeing the object consistency
	 * in the EntityManagerFactory.
	 * 
	 * @param transacted
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (queryProvider == null) {
			Assert.notNull(entityManagerFactory);
			// Assert.hasLength(queryString);
		}
		// making sure that the appropriate (JPA) query provider is set
		else {
			Assert.isTrue(queryProvider != null, "JPA query provider must be set");
		}
	}

	/**
	 * @param queryString
	 *            JPQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * @param queryProvider
	 *            JPA query provider
	 */
	public void setQueryProvider(JpaQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	@Override
	protected void doOpen() throws Exception {
		super.doOpen();
		entityManager = entityManagerFactory.createEntityManager(jpaPropertyMap);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain an EntityManager");
		}
		// set entityManager to queryProvider, so it participates
		// in JpaPagingItemReader's managed transaction
		if (queryProvider != null) {
			queryProvider.setEntityManager(entityManager);
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doReadPage() {
		LOGGER.debug("Inside doReadPage method");
		EntityTransaction tx = null;

		if (transacted) {
			tx = entityManager.getTransaction();
			tx.begin();

			entityManager.flush();
			entityManager.clear();
		} // end if

		Query query = createQuery().setMaxResults(getPageSize());
		LOGGER.debug("QUERY to hit DB" + query.toString());
		if (parameterValues != null) {
			for (Map.Entry<String, Object> me : parameterValues.entrySet()) {
				query.setParameter(me.getKey(), me.getValue());
			}
		}

		if (results == null) {
			results = new CopyOnWriteArrayList<T>();
		} else {
			results.clear();
		}
		List<T> queryResultTest = query.getResultList();
		for (T entity : queryResultTest) {
			LOGGER.debug("fetched object " + entity.toString());
			OrderDetail od = (OrderDetail) entity;
			orderDetailStartKey = od.getOrderDetailKey();
		}
		if (!transacted) {
			List<T> queryResult = query.getResultList();
			for (T entity : queryResult) {
				entityManager.detach(entity);
				results.add(entity);
			} // end if
		} else {
			results.addAll(query.getResultList());
			tx.commit();
		} // end if

		LOGGER.debug("Record fetched " + results);
		LOGGER.debug("exit from  doReadPage method");
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
	}

	@Override
	protected void doClose() throws Exception {
		entityManager.close();
		super.doClose();
	}

	
	
	
	public static Long getTrainingEntitlementKey() {
		return trainingEntitlementKey;
	}

	public static void setTrainingEntitlementKey(Long trainingEntitlementKey) {
		TrainingEntitlementDeltaItemReader.trainingEntitlementKey = trainingEntitlementKey;
	}

	public static Long getOrderDetailStartKey() {
		return orderDetailStartKey;
	}

	public static void setOrderDetailStartKey(Long orderDetailStartKey) {
		TrainingEntitlementDeltaItemReader.orderDetailStartKey = orderDetailStartKey;
	}

	public int getMaxPageSize() {
		LOGGER.info("Enter into TrainingEntitlementDeltaItemReader-getMaxPagrSize() method");
		Query query = entityManager.createQuery("select count(c.orderDetail)  from TrainingEntitlement c "
				+ "left join OrderDetail od on od.orderDetailKey=c.orderDetail "
				+ "left join OrderHeader oh on od.orderHeader.orderHdrKey=oh.orderHdrKey "
				+ "left join Codes it on c.itemType.codeKey=it.codeKey and trim(it.codeValue)='Training Backlog' "
				+ "left join Codes ct on c.statusCd.codeKey=ct.codeKey "
				+ "where ct.codeValue='ACTIVE' and it.codeValue is not null and (trunc(od.contractStartDt)<=trunc(sysdate) or trunc(od.contractEndDt)<=trunc(sysdate)) ");
		int count = ((Long) query.getSingleResult()).intValue();
		LOGGER.info("getMaxPageSize:" + count);
		return count;
	}
}
