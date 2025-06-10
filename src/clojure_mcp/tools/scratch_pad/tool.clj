(ns clojure-mcp.tools.scratch-pad.tool
  "Implementation of the scratch-pad tool using the tool-system multimethod approach."
  (:require
   [clojure-mcp.tool-system :as tool-system]
   [clojure-mcp.tools.scratch-pad.core :as core]
   [clojure.tools.logging :as log]
   [clojure.walk]))

(defn get-scratch-pad
  "Gets the current scratch pad data from the nrepl-client.
   Returns an empty map if no data exists yet."
  [nrepl-client-atom]
  (get @nrepl-client-atom ::scratch-pad {}))

(defn update-scratch-pad!
  "Updates the scratch pad data in the nrepl-client-atom."
  [nrepl-client-atom f & args]
  (apply swap! nrepl-client-atom update ::scratch-pad f args))

(defmethod tool-system/tool-name :scratch-pad [_]
  "scratch_pad")

(defmethod tool-system/tool-description :scratch-pad [_]
  "A persistent scratch pad for storing structured data between tool calls. Accepts any JSON value (objects, arrays, strings, numbers, booleans, null) and stores them at nested paths using set_path, get_path, delete_path operations.

THIS IS YOUR GO-TO TOOL FOR PLANNING.

Your persistent workspace for planning, organizing thoughts, and maintaining state across tool invocations.

This tool can be used to:
 * develop and track multi-step plans
 * maintain todo lists and task tracking
 * store intermediate results between operations
 * keep notes about your current approach or strategy
 * maintain a list of files or resources to process
 * build up complex data structures incrementally
 * share context between different tool calls
 * any other persistent storage needs during your work

The scratch pad is for persistent storage and state management, not for formatting output. Display information directly in your chat responses. However, when the scratch pad contains user-relevant state (like todo lists or tracked progress), you should retrieve and display updates after modifications.

Use the scratch pad when you need to:
- Track progress across multiple tool calls
- Build data structures incrementally  
- Maintain state between operations
- Store intermediate results for later use

Don't use the scratch pad to:
- Format text for display
- Store static information just to retrieve and show it
- Replace direct chat responses

CORE OPERATIONS:
- set_path: Store a value at a path, returns the parent container
- get_path: Retrieve a value from a path, returns the value or nil
- delete_path: Remove a value at a path, returns a confirmation message
- inspect: Display the entire structure (or a specific path) with truncation at specified depth

TRACKING PLANS WITH TODO LISTS:

Recommended todo item structure:
{
  task: \"Description of the task\",
  done: false,
  priority: \"high\", // optional: \"high\", \"medium\", \"low\"
  context: \"Additional details\", // optional
  subtasks: [  // optional: array of subtask objects
    {task: \"Subtask 1\", done: false},
    {task: \"Subtask 2\", done: true}
  ]
}

First add multiple todo items at once as an array:
- Entire array:
  op: set_path
  path: [\"todos\"]
  value: [
    {task: \"Write tests\", done: false, priority: \"high\"},
    {task: \"Review PR\", done: false, priority: \"high\"},
    {task: \"Update docs\", done: false, priority: \"medium\", subtasks: [
      {task: \"Update API docs\", done: false},
      {task: \"Update README\", done: false}
    ]}
  ]
  explanation: Adding multiple todos at once

Adding todo items:
- First item:
  op: set_path
  path: [\"todos\", 3]
  value: {task: \"Run tests\", done: false}
  explanation: Adding write tests task

- Next item:
  op: set_path
  path: [\"todos\", 4]
  value: {task: \"Notify user\", done: false, priority: \"high\"}
  explanation: Adding high priority task

Checking off completed tasks:
- Mark as done:
  op: set_path
  path: [\"todos\", 0, \"done\"]
  value: true
  explanation: Completed writing tests

- Mark subtask as done:
  op: set_path
  path: [\"todos\", 2, \"subtasks\", 0, \"done\"]
  value: true
  explanation: Completed API docs update

Adding a new todo to the array:
- Append to array:
  op: set_path
  path: [\"todos\", 3]
  value: {task: \"Deploy to production\", done: false, priority: \"high\"}
  explanation: Adding deployment task

Viewing todos:
- All todos:
  op: get_path
  path: [\"todos\"]
  explanation: Get all todo items

- Specific task:
  op: get_path
  path: [\"todos\", 0]
  explanation: Checking first task details

- View with depth limit:
  op: inspect
  path: [\"todos\"]
  depth: 2
  explanation: View todos with limited nesting")

(defmethod tool-system/tool-schema :scratch-pad [_]
  {:type "object"
   :properties {"op" {:type "string"
                      :enum ["set_path" "get_path" "delete_path" "inspect"]
                      :description "The operation to perform either\n * set_path: set a value at a path\n * get_path: retrieve a value at a path\n * delete_path: remove the value AND the leaf key from the data structure\n * inspect: view the datastructure (or a specific path within it) up to a certain depth"}
                "path" {:type "array"
                        :items {:type ["string" "number"]}
                        :description "Path to the data location (array of string or number keys) this works for all operations including inspect"}
                "value" {:description "Value to store (for set_path). Can be any JSON value: object, array, string, number, boolean, or null."
                         :type ["object" "array" "string" "number" "boolean" "null"]}
                "explanation" {:type "string"
                               :description "Explanation of why this operation is being performed"}
                "depth" {:type "number"
                         :description "(Optional) For inspect operation: Maximum depth to display (default: 5). Must be a positive integer."}}
   :required ["op" "explanation"]})

(defmethod tool-system/validate-inputs :scratch-pad [{:keys [nrepl-client-atom]} inputs]
  ;; convert set_path path nil -> delete_path path
  ;; this can prevent nil values from being in the data?
  (let [inputs (if (and
                    (nil? (:value inputs))
                    (= (:op inputs) "set_path")
                    (:path inputs))
                 (assoc inputs :op "delete_path")
                 inputs)
        {:keys [op path value explanation depth]} inputs]

    ;; Check required parameters
    (when-not op
      (throw (ex-info "Missing required parameter: op" {:inputs inputs})))

    (when-not explanation
      (throw (ex-info "Missing required parameter: explanation" {:inputs inputs})))

    ;; Validate operation
    (when-not (#{"set_path" "get_path" "delete_path" "inspect"} op)
      (throw (ex-info "Invalid operation. Must be one of: set_path, get_path, delete_path, inspect"
                      {:op op :inputs inputs})))

    ;; Operation-specific validation
    (case op
      "set_path" (do
                   (when-not path
                     (throw (ex-info "Missing required parameter for set_path: path" {:inputs inputs})))
                   (when-not (contains? inputs :value)
                     (throw (ex-info "Missing required parameter for set_path: value" {:inputs inputs}))))
      ("get_path" "delete_path") (when-not path
                                   (throw (ex-info (str "Missing required parameter for " op ": path")
                                                   {:inputs inputs})))
      "inspect" (when depth
                  (when-not (and (number? depth) (integer? depth) (pos? depth))
                    (throw (ex-info "Depth must be a positive integer greater than 0"
                                    {:depth depth :inputs inputs})))))

    ;; Validate path has at least one element when provided
    (when (and path (empty? path) (not= op "inspect"))
      (throw (ex-info "Path must have at least one element" {:path path :inputs inputs})))

    ;; Validate path elements are strings or numbers
    (when path
      (doseq [element path]
        (when-not (or (string? element) (number? element))
          (throw (ex-info "Path elements must be strings or numbers"
                          {:element element :type (type element) :path path})))))

    ;; Convert path to vector if needed (MCP provides as array)
    ;; And ensure depth is provided with default value for inspect
    (cond-> inputs
      path (assoc :path (vec path))
      (and (= op "inspect") (nil? depth)) (assoc :depth 5))))

(defmethod tool-system/execute-tool :scratch-pad [{:keys [nrepl-client-atom]} {:keys [op path value explanation depth]}]
  (try
    (let [current-data (get-scratch-pad nrepl-client-atom)
          exec-result (case op
                        "set_path" (let [{:keys [data result]} (core/execute-set-path current-data path value)]
                                     (update-scratch-pad! nrepl-client-atom (constantly data))
                                     ;; Add parent value to result
                                     (let [parent-path (butlast path)
                                           parent-value (if (empty? parent-path)
                                                          data
                                                          (get-in data parent-path))]
                                       (assoc result :parent-value parent-value)))

                        "get_path" (:result (core/execute-get-path current-data path))

                        "delete_path" (let [{:keys [data result]} (core/execute-delete-path current-data path)]
                                        (update-scratch-pad! nrepl-client-atom (constantly data))
                                        result)

                        "inspect" (:result (core/execute-inspect current-data depth path)))]
      {:result exec-result
       :explanation explanation
       :error false})
    (catch Exception e
      {:error true
       :message (str "Error executing scratch pad operation: " (.getMessage e))})))

;; this is convoluted this can be stream lined as most of this is imply echoing back what was sent
(defmethod tool-system/format-results :scratch-pad [_ {:keys [error message result explanation]}]
  (if error
    {:result [message]
     :error true}
    (cond
      ;; set_path - return pprinted parent value
      (:stored-at result)
      {:result [(with-out-str (clojure.pprint/pprint (:parent-value result)))]
       :error false}

      ;; get_path - return pprinted value
      (contains? result :value)
      {:result [(or (:pretty-value result) "nil")]
       :error false}

      ;; delete_path - return removed message only
      (:removed-from result)
      {:result [(str "Removed value at path " (:removed-from result))]
       :error false}

      ;; inspect - return pprinted truncated view only
      (:tree result)
      {:result [(:tree result)]
       :error false})))

;; this is needed because of the special handling of edn in the default handler
(defmethod tool-system/registration-map :scratch-pad [tool-config]
  {:name (tool-system/tool-name tool-config)
   :description (tool-system/tool-description tool-config)
   :schema (tool-system/tool-schema tool-config)
   :tool-fn (fn [_ params callback]
              (try
                (let [converted-params (tool-system/convert-java-collections params)
                      keywordized-params (-> converted-params
                                             (dissoc "value")
                                             tool-system/keywordize-keys-preserve-underscores)
                      {:keys [result error]}
                      (->> (get converted-params "value")
                           (assoc keywordized-params :value)
                           (tool-system/validate-inputs tool-config)
                           (tool-system/execute-tool tool-config)
                           (tool-system/format-results tool-config))]
                  (callback result error))
                (catch Exception e
                  (log/error e)
                  ;; On error, create a sequence of error messages
                  (let [error-msg (or (ex-message e) "Unknown error")
                        data (ex-data e)
                        ;; Construct error messages sequence
                        error-msgs (cond-> [error-msg]
                                     ;; Add any error-details from ex-data if available
                                     (and data (:error-details data))
                                     (concat (if (sequential? (:error-details data))
                                               (:error-details data)
                                               [(:error-details data)])))]
                    (callback error-msgs true)))))})

(defn create-scratch-pad-tool [nrepl-client-atom]
  {:tool-type :scratch-pad
   :nrepl-client-atom nrepl-client-atom})

(defn scratch-pad-tool
  "Returns the registration map for the scratch pad tool.
   
   Parameters:
   - nrepl-client-atom: Atom containing the nREPL client"
  [nrepl-client-atom]
  (tool-system/registration-map (create-scratch-pad-tool nrepl-client-atom)))

(comment
  ;; Usage examples with JSON values:

  ;; Store a simple string
  {:op "set_path"
   :path ["user" "name"]
   :value "Alice"
   :explanation "Setting user name"}

  ;; Store an object (JSON object becomes Clojure map with string keys)
  {:op "set_path"
   :path ["config"]
   :value {"theme" "dark" "fontSize" 14}
   :explanation "Storing user preferences"}

  ;; Store an array
  {:op "set_path"
   :path ["tags"]
   :value ["clojure" "mcp" "tools"]
   :explanation "Setting project tags"}

  ;; === TODO LIST WORKFLOW EXAMPLE WITH ARRAYS ===

  ;; 1. Initialize todos as empty array
  {:op "set_path"
   :path ["todos"]
   :value []
   :explanation "Initialize empty todo list"}

  ;; 2. Add first todo item to array
  {:op "set_path"
   :path ["todos" 0]
   :value {"task" "Implement user authentication" "done" false "priority" "high"}
   :explanation "Starting authentication work"}

  ;; 3. Add multiple todos at once as array
  {:op "set_path"
   :path ["todos"]
   :value [{"task" "Implement authentication" "done" false "priority" "high"}
           {"task" "Write unit tests" "done" false "priority" "medium"
            "subtasks" [{"task" "Test login flow" "done" false}
                        {"task" "Test logout flow" "done" false}]}
           {"task" "Update documentation" "done" false}]
   :explanation "Setting up complete todo list"}

  ;; 4. Check off first task (boolean value)
  {:op "set_path"
   :path ["todos" 0 "done"]
   :value true
   :explanation "Completed authentication implementation"}

  ;; 5. Mark subtask as done
  {:op "set_path"
   :path ["todos" 1 "subtasks" 0 "done"]
   :value true
   :explanation "Completed login flow tests"}

  ;; 6. Add context to a task
  {:op "set_path"
   :path ["todos" 0 "context"]
   :value "Used OAuth2 with JWT tokens"
   :explanation "Adding implementation details"}

  ;; 7. Append new todo to array
  {:op "set_path"
   :path ["todos" 3]
   :value {"task" "Deploy to production" "done" false "priority" "high"}
   :explanation "Adding deployment task"}

  ;; 8. View all todos
  {:op "get_path"
   :path ["todos"]
   :explanation "Get all todo items"}

  ;; 9. View specific todo with subtasks
  {:op "get_path"
   :path ["todos" 1]
   :explanation "Get task with subtasks"}

  ;; 10. Inspect with depth limit
  {:op "inspect"
   :path ["todos"]
   :depth 2
   :explanation "View todos structure with limited depth"}

  ;; === OTHER OPERATIONS ===

  ;; Get a specific value
  {:op "get_path"
   :path ["user" "name"]
   :explanation "Retrieving user name"}

  ;; Store nested data
  {:op "set_path"
   :path ["metrics" "performance"]
   :value {"cpu" 45.2 "memory" 78 "disk" 92}
   :explanation "Recording system metrics"}

  ;; Remove a value
  {:op "delete_path"
   :path ["config" "old-setting"]
   :explanation "Removing deprecated config"})
