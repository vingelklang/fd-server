# fd-server

## Running Notes:

### `config.edn`

A `config.edn` is required to run the jar. It is not included with this source.

Current format: 

```clojure
{:email-account ""
 :email-password ""
 :email-recipients [""]}
```

Note that this schema has the potential of being outdated. 

### Postgres

A Postgres DB URL needs to be provided as an ENV variable.

Example of running the jar with the ENV set:

```shell
DATABASE_URL=postgresql://localhost/fd_server_db java -jar fd_server.jar
```

## Dev

1. Install leiningen: 'sudo apt-get install leiningen'
2. Install postgresql: 'sudo apt-get install postgresql'
3. Set up postgresql as systemctl: 'sudo systemctl start postgresql@11-main'
4. Start lein: 'lein run'

