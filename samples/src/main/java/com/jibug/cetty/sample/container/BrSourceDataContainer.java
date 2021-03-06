package com.jibug.cetty.sample.container;

import com.jibug.cetty.sample.entity.domain.MlGoodsBr;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BrSourceDataContainer {

    private ConcurrentLinkedQueue<MlGoodsBr> queueBr;
    public BrSourceDataContainer() {
        queueBr =  new ConcurrentLinkedQueue<>();
    }
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock r = rwLock.readLock();
    private final Lock w = rwLock.writeLock();

    public MlGoodsBr poll() {
        return queueBr.poll();
    }

    /**
     * add
     */
    public void offer(MlGoodsBr mlGoodsBr) {
       this.queueBr.offer(mlGoodsBr);
    }

    /**
     * 删除
     */
    public void remove(MlGoodsBr mlGoodsBr) {
       this.queueBr.remove(mlGoodsBr);
    }

    /**
     * size
     */
    public int size() {
        return queueBr.size();
    }

    public boolean isEmpty(){
        return this.queueBr.isEmpty();
    }

    public boolean contains(MlGoodsBr mlGoodsBr){
        return this.queueBr.contains(mlGoodsBr);
    }

    public Iterator<MlGoodsBr> iterator(){
        return this.queueBr.iterator();
    }

}
