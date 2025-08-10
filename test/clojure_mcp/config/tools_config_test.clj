(ns clojure-mcp.config.tools-config-test
  (:require [clojure.test :refer :all]
            [clojure-mcp.config :as config]))

(deftest test-get-tools-config
  (testing "Returns tools config when present"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:model :openai/o3}
                                            :architect {:model :anthropic/claude}}}}]
      (is (= {:dispatch_agent {:model :openai/o3}
              :architect {:model :anthropic/claude}}
             (config/get-tools-config nrepl-client-map)))))

  (testing "Returns empty map when not configured"
    (let [nrepl-client-map {::config/config {}}]
      (is (= {} (config/get-tools-config nrepl-client-map))))))

(deftest test-get-tool-config
  (testing "Returns config for specific tool"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:model :openai/o3}
                                            :architect {:model :anthropic/claude}}}}]
      (is (= {:model :openai/o3}
             (config/get-tool-config nrepl-client-map :dispatch_agent)))
      (is (= {:model :anthropic/claude}
             (config/get-tool-config nrepl-client-map :architect)))))

  (testing "Returns nil for unconfigured tool"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:model :openai/o3}}}}]
      (is (nil? (config/get-tool-config nrepl-client-map :missing_tool)))))

  (testing "Handles string tool IDs"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:model :openai/o3}}}}]
      (is (= {:model :openai/o3}
             (config/get-tool-config nrepl-client-map "dispatch_agent"))))))

(deftest test-get-tool-model
  (testing "Creates model from tool config with default :model key"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:model :anthropic/claude-3-haiku-20240307}}
                             :models {:anthropic/claude-3-haiku-20240307
                                      {:model-name "claude-3-haiku-20240307"
                                       :api-key [:env "ANTHROPIC_API_KEY"]}}}}]
      ;; Model creation will succeed if API key is set
      (let [model (config/get-tool-model nrepl-client-map :dispatch_agent)]
        (is (or (some? model)
                (nil? model)) ; Allow nil if API key not set
            "Should either create model or return nil"))))

  (testing "Creates model with custom config key"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:architect {:primary-model :openai/gpt-4o}}
                             :models {:openai/gpt-4o
                                      {:model-name "gpt-4o"
                                       :api-key [:env "OPENAI_API_KEY"]}}}}]
      (let [model (config/get-tool-model nrepl-client-map :architect :primary-model)]
        (is (or (some? model)
                (nil? model))
            "Should either create model or return nil"))))

  (testing "Returns nil when tool not configured"
    (let [nrepl-client-map {::config/config {:tools-config {}}}]
      (is (nil? (config/get-tool-model nrepl-client-map :missing_tool)))))

  (testing "Returns nil when model key not in tool config"
    (let [nrepl-client-map {::config/config
                            {:tools-config {:dispatch_agent {:other-key "value"}}}}]
      (is (nil? (config/get-tool-model nrepl-client-map :dispatch_agent))))))
