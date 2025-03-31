# onepassword-clj

[![Clojars Project](https://img.shields.io/clojars/v/com.github.gpellis/onepassword-clj.svg)](https://clojars.org/com.github.gpellis/onepassword-clj) <!-- Update when deployed -->

A Clojure library for interacting with the 1Password CLI (`op`).

## Rationale

Provides idiomatic Clojure functions to read data (items, vaults, documents, secrets) from 1Password via its command-line tool. Returns data as namespaced Clojure maps.

## Prerequisites

*   **1Password CLI (`op`)**: You MUST have the `op` command-line tool installed and available on your system's `PATH`. Download and install it from the [1Password Developer Documentation](https://developer.1password.com/docs/cli/get-started/).
*   **Authentication**: You MUST be signed in to your 1Password account using the CLI before using this library. Typically, you run `op account add` followed by `op signin` interactively. This library *does not* handle the sign-in process.

## Installation

Add the following dependency to your `deps.edn` file:

```clojure
com.github.gpellis/onepassword-clj {:mvn/version "RELEASE"} ; Replace RELEASE with the latest version```

Or Leiningen `project.clj`:

```clojure
[com.github.gpellis/onepassword-clj "RELEASE"] ; Replace RELEASE with the latest version```

## Usage

(require '[com.github.gpellis.onepassword-clj.core :as op])

;; --- Listing ---

;; List all vaults
(op/list-vaults)
;; => [{:com.github.gpellis.onepassword-clj.vault/id "vault_id_1",
;;      :com.github.gpellis.onepassword-clj.vault/name "Personal", ...}
;;     ...]
;; OR on error: {:error :cli-execution, :cmd [...], :exit 1, :stderr "..."}

;; List all items (logins, secure notes, etc.)
(op/list-items)
;; => [{:com.github.gpellis.onepassword-clj.item/id "item_id_1",
;;      :com.github.gpellis.onepassword-clj.item/title "GitHub", ...}
;;     ...]

;; List items in a specific vault
(op/list-items {:vault "Private"}) ;; By name or ID
;; => [...]

;; --- Getting Specific Entities ---

;; Get vault details by ID or name
(op/get-vault "vault_id_1")
;; => {:com.github.gpellis.onepassword-clj.vault/id "vault_id_1", ...}
;; OR {:error :cli-execution, ...} if not found/error

;; Get item details by ID or name
(op/get-item "item_id_xyz")
;; => {:com.github.gpellis.onepassword-clj.item/id "item_id_xyz",
;;     :com.github.gpellis.onepassword-clj.item/title "...",
;;     :com.github.gpellis.onepassword-clj.item/fields [...], ...}

;; Get document details by ID or name
(op/get-document "document_abc")
;; => {:com.github.gpellis.onepassword-clj.document/id "document_abc", ...}

;; --- Reading Secrets ---

;; Get the content of a document
;; Note: This returns the raw document content as a string
(op/get-document-content "document_abc")
;; => "Content of the document...\n"
;; OR {:error :cli-execution, ...}

;; Read a specific secret using the 'op://' reference
(op/read-secret "op://Personal/github/password")
;; => "mysecretpassword"
;; OR {:error :cli-execution, ...}

;; --- Other ---

;; Check which account is active
(op/whoami)
;; => {:com.github.gpellis.onepassword-clj.user/url "my.1password.com",
;;     :com.github.gpellis.onepassword-clj.user/email "user@example.com",
;;     :com.github.gpellis.onepassword-clj.user/id "user_id", ...}


;; --- Error Handling ---
;; Functions return maps. Success is indicated by the absence of an `:error` key.
;; If an error occurs (CLI exit code != 0, JSON parsing fails), a map like the following is returned:
;; {:error :error-type ; e.g., :cli-execution, :json-parsing
;;  :message "A human-readable message"
;;  :details {...}} ; Context-dependent details, like :cmd, :exit, :stderr, :raw-output

## Data Format

Successful results are returned as Clojure maps (or vectors of maps) with keywords namespaced under com.github.gpellis.onepassword-clj.* (e.g., :com.github.gpellis.onepassword-clj.item/id, :com.github.gpellis.onepassword-clj.vault/name).

Timestamp fields are currently returned as ISO 8601 string values as provided by the op CLI.

## Development
Run tests:

clojure -X:test

## Contributing
Issues and Pull Requests are welcome! Please ensure tests pass and adhere to conventional commits if possible.


