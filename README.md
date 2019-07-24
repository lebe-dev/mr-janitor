# Mr.Janitor

Deal with old backup and log files in a smart way.


## How to use

Create configuration file `janitor-conf`:

```
cp janitor.conf-distrib janitor.conf 
```

### Dry run

Do not delete anything, just show items for clean up.

```
java -jar janitor dry-run
```

Example output:

```
profile 'site'
- path: '/backups/super-important-site'
- storage-unit: directory
- keep copies: 3
directory items for clean up (4):
- '2019-07-19'
  - files: 51
  - valid: true
  - last-modified: Fri Jul 19 20:25:53 MSK 2019
- '2019-07-20'
  - files: 50
  - valid: false
  - last-modified: Sat Jul 20 20:23:48 MSK 2019
---
items total: 3

```
