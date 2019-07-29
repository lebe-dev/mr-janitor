# Mr.Janitor

Deal with old backup and log files in a smart way.


## How to use

Create configuration file `janitor-conf`:

```
cp janitor.conf-distrib janitor.conf 
```

### Clean up

Clean old items:

```bash
java -jar janitor.jar cleanup
```

### Dry run

Do not delete anything, just show items for clean up.

```
java -jar janitor.jar dry-run
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

## Smart checks

Each directory or file item can be validated.

### 1. Directory item - Total size at least as in previous item

Current directory item has total file size at least as previous item.

How to enable:

```
item-validation {
    size-at-least-as-previous = true
}
```

### 2. Directory item - File items count at least as in previous item

Current directory item contains files amount at least as in previous directory.

How to enable:

```
item-validation {
    qty-at-least-as-previous-valid = true
}
```

### 3. File item - Size should be at least as for previous file item



How to enable:

```
item-validation {
    size-at-least-as-previous = true
}
```
