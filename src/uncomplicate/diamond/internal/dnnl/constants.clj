;;   Copyright (c) Dragan Djuric. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns uncomplicate.diamond.internal.dnnl.constants
  (:require [uncomplicate.commons.utils :refer [dragan-says-ex]])
  (:import org.bytedeco.dnnl.global.dnnl))

(defn dec-status [^long status]
  (case status
    0 :success
    1 :out-of-memory
    2 :invalid-arguments
    3 :unimplemented
    4 :iterator-ends
    5 :runtime-error
    6 :not-required
    :unknown))

(def ^:const dnnl-engine-kind
  {:cpu dnnl/dnnl_cpu
   :gpu dnnl/dnnl_gpu
   :any dnnl/dnnl_any_engine})

(defn dec-engine-kind [^long kind]
  (case kind
    1 :cpu
    2 :gpu
    0 :any
    :unknown))

(def ^:const dnnl-stream-flags
  {:default-order dnnl/dnnl_stream_default_flags
   :in-order dnnl/dnnl_stream_in_order
   :out-of-order dnnl/dnnl_stream_out_of_order
   :default-flags dnnl/dnnl_stream_default_flags})

(defn dec-primitive-kind [^long primitive-kind]
  (case primitive-kind
    1 :reorder
    2 :shuffle
    3 :concat
    4 :sum
    5 :convolution
    6 :deconvolution
    7 :eltwise
    8 :softmax
    9 :pooling
    10 :lrn
    11 :batch-normalization
    12 :layer-normalization
    13 :inner-product
    14 :rnn
    15 :gemm
    16 :binary
    0 :undef
    (dragan-says-ex "Unknown primitive kind." {:primitive-kind primitive-kind})))

(defn dec-format [^long format]
  (case format
    0 :undef
    1 :any
    2 :a
    3 :ab
    4 :abc
    5 :abcd
    6 :abcde
    7 :abcdef
    8 :abdec
    10 :acb
    11 :acbde
    12 :acdeb
    13 :ba
    14 :bac
    15 :bacd
    16 :bca
    17 :bcda
    18 :bcdea
    19 :cba
    20 :cdba
    21 :cdeba
    22 :decab
    (dragan-says-ex (format "%s format." (if (< 22 format 132) "Opaque" "Unknown"))
                    {:format format})))

(def ^:const dnnl-format
  {:undef dnnl/dnnl_format_tag_undef
   :any dnnl/dnnl_format_tag_any
   :blocked dnnl/dnnl_blocked
   :x dnnl/dnnl_x
   :nc dnnl/dnnl_nc
   :cn dnnl/dnnl_cn
   :tn dnnl/dnnl_tn
   :nt dnnl/dnnl_nt
   :ncw dnnl/dnnl_ncw
   :nwc dnnl/dnnl_nwc
   :nchw dnnl/dnnl_nchw
   :nhwc dnnl/dnnl_nhwc
   :chwn dnnl/dnnl_chwn
   :ncdhw dnnl/dnnl_ncdhw
   :ndhwc dnnl/dnnl_ndhwc
   :oi dnnl/dnnl_oi
   :io dnnl/dnnl_io
   :oiw dnnl/dnnl_oiw
   :owi dnnl/dnnl_owi
   :wio dnnl/dnnl_wio
   :iwo dnnl/dnnl_iwo
   :oihw dnnl/dnnl_oihw
   :hwio dnnl/dnnl_hwio
   :ohwi dnnl/dnnl_ohwi
   :ihwo dnnl/dnnl_ihwo
   :iohw dnnl/dnnl_iohw
   :oidhw dnnl/dnnl_oidhw
   :dhwio dnnl/dnnl_dhwio
   :odhwi dnnl/dnnl_odhwi
   :idhwo dnnl/dnnl_idhwo
   :goiw dnnl/dnnl_goiw
   :goihw dnnl/dnnl_goihw
   :hwigo dnnl/dnnl_hwigo
   :giohw dnnl/dnnl_giohw
   :goidhw dnnl/dnnl_goidhw
   :tnc dnnl/dnnl_tnc
   :ntc dnnl/dnnl_ntc
   :ldnc dnnl/dnnl_ldnc
   :ldigo dnnl/dnnl_ldigo
   :ldgoi dnnl/dnnl_ldgoi
   :ldgo dnnl/dnnl_ldgo
   :a dnnl/dnnl_a
   :ab dnnl/dnnl_ab
   :abc dnnl/dnnl_abc
   :abcd dnnl/dnnl_abcd
   :abcde dnnl/dnnl_abcde
   :abcdef dnnl/dnnl_abcdef
   :abdec dnnl/dnnl_abdec
   :acb dnnl/dnnl_acb
   :acbde dnnl/dnnl_acbde
   :acdb dnnl/dnnl_acdb
   :acdeb dnnl/dnnl_acdeb
   :ba dnnl/dnnl_ba
   :bac dnnl/dnnl_bac
   :bacd dnnl/dnnl_bacd
   :bca dnnl/dnnl_bca
   :bcda dnnl/dnnl_bcda
   :bcdea dnnl/dnnl_bcdea
   :cba dnnl/dnnl_cba
   :cdba dnnl/dnnl_cdba
   :cdeba dnnl/dnnl_cdeba
   :decab dnnl/dnnl_decab})

(defn dec-data-type [^long data-type]
  (case data-type
    3 :float
    1 :f16
    2 :bf16
    4 :int
    5 :byte
    6 :u8
    0 :undef
    (dragan-says-ex "Unknown data type." {:data-type data-type})))

(def ^:const dnnl-data-type
  {:float dnnl/dnnl_f32
   Float/TYPE dnnl/dnnl_f32
   :f16 dnnl/dnnl_f16
   :bf16 dnnl/dnnl_bf16
   :int dnnl/dnnl_s32
   Integer/TYPE dnnl/dnnl_s32
   :byte dnnl/dnnl_s8
   Byte/TYPE dnnl/dnnl_s8
   :u8 dnnl/dnnl_u8
   :undef dnnl/dnnl_data_type_undef})

(def ^:const dnnl-forward-prop-kind
  {:training dnnl/dnnl_forward_training
   :inference dnnl/dnnl_forward_inference
   :scoring dnnl/dnnl_forward_scoring})

(def ^:const dnnl-backward-prop-kind
  {:backward dnnl/dnnl_backward
   :data dnnl/dnnl_backward_data
   :weights dnnl/dnnl_backward_weights
   :bias dnnl/dnnl_backward_bias})

(def ^:const dnnl-eltwise-alg-kind
  {:relu dnnl/dnnl_eltwise_relu
   :tanh dnnl/dnnl_eltwise_tanh
   :elu dnnl/dnnl_eltwise_elu
   :square dnnl/dnnl_eltwise_square
   :abs dnnl/dnnl_eltwise_abs
   :sqrt dnnl/dnnl_eltwise_sqrt
   :linear dnnl/dnnl_eltwise_linear
   :bounded-relu dnnl/dnnl_eltwise_bounded_relu
   :soft-relu dnnl/dnnl_eltwise_soft_relu
   :logistic dnnl/dnnl_eltwise_logistic
   :sigmoid dnnl/dnnl_eltwise_logistic
   :exp dnnl/dnnl_eltwise_exp
   :gelu dnnl/dnnl_eltwise_gelu})

(defn entry-bytes ^long [data-type]
  (case data-type
    :float Float/BYTES
    :f16 2
    :bf16 2
    :int Integer/BYTES
    :byte 1
    :u8 1
    (dragan-says-ex "unknown data type" {:data-type data-type})))
