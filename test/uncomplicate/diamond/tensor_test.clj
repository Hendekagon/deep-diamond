(ns uncomplicate.diamond.tensor-test
  (:require [midje.sweet :refer [facts throws =>]]
            [uncomplicate.commons
             [core :refer [with-release]]]
            [uncomplicate.neanderthal
             [core :refer [asum view transfer! native]]
             [real :refer [entry! entry]]
             [block :refer [buffer]]]
            [uncomplicate.diamond.tensor :refer :all])
  (:import clojure.lang.ExceptionInfo))

(defn test-tensor [factory]
  (facts
   "Basic tensor tests"
   (with-release [tz (tensor factory [2 3 4 5] :float :nchw)]
     (asum (view tz)) => 0.0
     (asum (entry! (view tz) 1)) => 120.0
     (shape tz) => [2 3 4 5])))

(defn test-transformer [factory]
  (facts
   "Tensor transformator"
   (with-release [tz-x (tensor factory [2 3 4 5] :float :nchw)
                  tz-y (tensor factory [2 3 4 5] :float :nhwc)
                  transform (transformer tz-x tz-y)]
     (entry (native (transfer! (range) (view tz-x))) 119) => 119.0
     (entry (native (view tz-y)) 119) => 0.0
     (buffer (input transform)) => (buffer tz-x)
     (buffer (output transform)) => (buffer tz-y)
     (transform) => tz-y
     (entry (native (view tz-y)) 119) => 119.0)))

(defn test-pull-different [factory]
  (facts
   "Tensor pull connector with different destination"
   (with-release [tz-x (tensor factory [2 3 4 5] :float :nchw)
                  tz-y-desc (desc [2 3 4 5] :float :nhwc)
                  connection (connector tz-x tz-y-desc)]
     (entry (transfer! (range) (view tz-x)) 119) => 119.0
     (identical? (buffer (input connection)) (buffer tz-x)) => true
     (identical? (buffer (input connection)) (buffer (output connection))) => false
     (entry (view (connection)) 119) => 119.0)))

(defn test-pull-same [factory]
  (facts
   "Tensor pull connector with the same destination"
   (with-release [tz-x (tensor factory [2 3 4 5] :float :nchw)
                  tz-y-desc (desc [2 3 4 5] :float :nchw)
                  connection (connector tz-x tz-y-desc)]
     (entry (transfer! (range) (view tz-x)) 119) => 119.0
     (identical? (buffer (input connection)) (buffer tz-x)) => true
     (identical? (buffer (input connection)) (buffer (output connection))) => true
     (entry (view (connection)) 119) => 119.0)))

(defn test-push-different [factory]
  (facts
   "Tensor push connector with different destination"
   (with-release [tz-y (tensor factory [2 3 4 5] :float :nchw)
                  tz-x-desc (desc [2 3 4 5] :float :nhwc)
                  connection (connector tz-x-desc tz-y)]
     (entry (transfer! (range) (view (input connection))) 119) => 119.0
     (identical? (buffer (output connection)) (buffer tz-y)) => true
     (identical? (buffer (input connection)) (buffer (output connection))) => false
     (entry (view (connection)) 119) => 119.0)))

(defn test-push-same [factory]
  (facts
   "Tensor push connector with the same destination"
   (with-release [tz-y (tensor factory [2 3 4 5] :float :nchw)
                  tz-x-desc (desc [2 3 4 5] :float :nchw)
                  connection (connector tz-x-desc tz-y)]
     (entry (transfer! (range) (view connection)) 119) => 119.0
     (identical? (buffer (output connection)) (buffer tz-y)) => true
     (identical? (buffer (input connection)) (buffer (output connection))) => true
     (entry (view (connection)) 119) => 119.0)))

(defn test-subtensor [factory]
  (facts "Test subtensors and offsets."
         (with-release [tz-x (tensor factory [6] :float :x)
                        sub-x (view-tz tz-x [2])
                        sub-y (view-tz tz-x (desc [1 3] :nc))
                        sub-z (view-tz tz-x 4)]
           (transfer! (range) tz-x)
           (seq tz-x) => [0.0 1.0 2.0 3.0 4.0 5.0]
           (seq sub-x) => [0.0 1.0]
           (seq sub-y) => [0.0 1.0 2.0]
           (seq sub-z) => [0.0 1.0 2.0 3.0]
           (uncomplicate.diamond.internal.dnnl.core/offset! (buffer sub-y) Float/BYTES);;TODO generalize
           (seq sub-y) => [1.0 2.0 3.0]
           (seq sub-x) => [0.0 1.0]
           (uncomplicate.diamond.internal.dnnl.core/offset! (buffer sub-z) Float/BYTES)
           (seq sub-z) => [1.0 2.0 3.0 4.0]
           (seq sub-x) => [0.0 1.0])))

(defn test-shuffler [factory]
  (facts "shuffler test."
         (with-release [tz-x (tensor factory [6 2] :float :nc)
                        tz-y (tensor factory [3 2] :float :cn)
                        shuff (shuffler tz-x tz-y)]
           (transfer! (range 1 13) tz-x)
           (seq tz-x) => (range 1.0 13.0)
           (seq tz-y) => [0.0 0.0 0.0 0.0 0.0 0.0]
           (shuff [0 2 1])
           (seq tz-y) => [1.0 5.0 3.0 2.0 6.0 4.0]
           (shuff [0 2 1 1]) => (throws ExceptionInfo)
           (shuff [0 2 8]) => (throws ExceptionInfo)
           (shuff [0 1]) => tz-y)))

(defn test-batcher [factory]
  (facts "batcher test."
         (with-release [tz-x (tensor factory [7 2] :float :nc)
                        tz-y (tensor factory [3 2] :float :cn)
                        batch (batcher tz-x tz-y 3)
                        batch-2 (batcher tz-x tz-y 2)]
           (transfer! (range 1 15) tz-x)
           (seq tz-x) => (range 1.0 15.0)
           (seq tz-y) => (repeat 6 0.0)
           (batch 0 0) => tz-y
           (seq tz-y) => [1.0 3.0 5.0 2.0 4.0 6.0]
           (transfer! (repeat 0) tz-y)
           (batch 1 0) => tz-y
           (seq tz-y) => [3.0 5.0 7.0 4.0 6.0 8.0]
           (transfer! (repeat 0) tz-y)
           (batch-2 1 1) => tz-y
           (seq tz-y) => [0.0 3.0 5.0 0.0 4.0 6.0]
           (batch 8) => (throws ExceptionInfo)
           (batch 0 -1) => (throws ExceptionInfo)
           (batch 7 -1) => (throws ExceptionInfo)
           (batch -1) => (throws ExceptionInfo))))

(test-tensor *diamond-factory*)
(test-transformer *diamond-factory*)
(test-pull-different *diamond-factory*)
(test-pull-same *diamond-factory*)
(test-push-different *diamond-factory*)
(test-push-same *diamond-factory*)
(test-subtensor *diamond-factory*)
(test-shuffler *diamond-factory*)
(test-batcher *diamond-factory*)
