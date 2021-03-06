package com.pzoom.mdsp.logfilling;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pzoom.mdsp.util.ConstData;

/**
 * zk状态 初始化CuratorFramework 读和写zk的JSON
 * 
 * @author chenbaoyu
 * 
 */
public class ZkState {
	public static final Logger LOG = LoggerFactory.getLogger(ZkState.class);

	private CuratorFramework curator = null;

	public ZkState(String offsetZkStr) {
		try {
			curator = newCurator(offsetZkStr);
			curator.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private CuratorFramework newCurator(String offsetZkStr) throws Exception {
		return CuratorFrameworkFactory.newClient(offsetZkStr,
				ConstData.SESSION_TIMEOUT, 150000, new RetryNTimes(
						ConstData.RETRY_TIMES, ConstData.RETRY_INTERVAL));
	}

	public CuratorFramework getCurator() {
		assert curator != null;
		return curator;
	}

	public void writeJSON(String path, Map<Object, Object> data) {
		LOG.info("Writing " + path + " the data " + data.toString());
		writeBytes(path,
				JSONValue.toJSONString(data).getBytes(Charset.forName("UTF-8")));
	}

	/**
	 * writeJson
	 * 
	 * @param path
	 * @param bytes
	 */
	public void writeBytes(String path, byte[] bytes) {
		try {
			if (curator.checkExists().forPath(path) == null) {
				curator.create().creatingParentsIfNeeded()
						.withMode(CreateMode.PERSISTENT).forPath(path, bytes);
			} else {
				curator.setData().forPath(path, bytes);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Map<Object, Object> readJSON(String path) {
		try {
			byte[] b = readBytes(path);
			if (b == null) {
				return null;
			}
			return (Map<Object, Object>) JSONValue
					.parse(new String(b, "UTF-8"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * readJson
	 * 
	 * @param path
	 * @return
	 */
	public byte[] readBytes(String path) {
		try {
			if (curator.checkExists().forPath(path) != null) {
				return curator.getData().forPath(path);
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		curator.close();
		curator = null;
	}

}
