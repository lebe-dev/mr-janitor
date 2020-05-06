# 🧹 Mr.Janitor

Deal with outdated files in a smart way.

На [русском](README-RU.md).

## Features

1. Cleanup outdated files

2. Support two file object types:
    - Directory
    - File

3. Data validation  
    - Size comparison
    - Md5 Check
    - Zip test
    - Custom command

4.Configurable cleanup policy

## How to use

Create configuration file `janitor.conf`:

```
cp janitor.conf-distrib janitor.conf 
```

## How to install

**1. Create app directory**

```
mkdir /opt/janitor
chown -R janitor.janitor /opt/janitor
chmod 750 /opt/janitor
```

**2. Create special user**

```shell script
useradd -d /opt/janitor janitor && chown -R janitor: /opt/janitor
```

**3. Add permissions to access directory with data**

```
setfacl -Rm u:janitor:rwx /backups
```

**4. Create startup script for Janitor**

Create file `/opt/janitor/janitor.sh` with content:

```bash
#!/bin/bash

cd /opt/janitor
java -jar janitor.jar cleanup
```

Add execution permission:

```shell script
chmod +x /opt/janitor/janitor.sh
```

**5. Add task to cron schedule**

```
# backups cleanup by janitor
0 19  *  *  *  janitor java -jar /opt/janitor/janitor.jar cleanup
```

Restart `crond`.

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

## Basic configuration

### 1. How many items to keep

You can define in any profile how many items to keep. How to do it:

```
someProfile {
    keep-items-quantity = 7
}
```

### 2. Profiles

Janitor support profiles.

#### Add new profile

1.Choose unique profile name

2.Put name in `profiles` property

It will look like:

```
profiles = ["YourShinyNewProfile"]
```

3.Define profile section as shown below:

```
YourShinyNewProfile {
    ...
}
```

Get inspiration from `janitor.conf-distrib` file.

## Smart checks

Janitor supports several checks for data validation.

For example: zip archives can be tested for integrity. Objects with md5 companion files can be checked with
hash comparison.

Property `unit` defines which [type](StorageUnits.md) of storage units should we use.

There are two types:

- `DIRECTORY` - directory with files
- `FILE` - files

Check more examples with comments in `janitor.conf-distrib`.

### 1. Directory item - Total size at least as in previous item

Current directory item has total size at least as previous directory item.

How to enable:

```
item-validation {
    directory {
        size-at-least-as-previous = true
    }
}
```

### 2. Directory item - File items quantity at least as in previous item

Current directory item contains files quantity at least as in previous directory.

How to enable:

```
item-validation {
    directory {
        files-qty-at-least-as-in-previous = true
    }
}
```

### 3. Directory Item - File item size should >= same file item in previous directory

```
item-validation {
    directory {
        file-size-at-least-as-in-previous = true
    }
}
```

### 4. File item - Size should be at least as for previous file item

How to enable:

```
item-validation {
    file {
        size-at-least-as-previous = true
    }
}
```

### 5. File item - MD5 file hash check

Janitor looking for .md5 file companion and compare hashes

How to enable:

```
item-validation {
    file {
        md5-file-check = true
    }
}
```

### 6. File item - Zip test

Check zip archive integrity

How to enable:

```
item-validation {
    file {
        zip-test = true
    }
}
```

### 7. File item - Log file exists

Janitor looking for .log companion file.

How to enable:

```
item-validation {
    file {
        log-file-exists = true
    }
}
```

### 8. File item - Custom validator command

You can specify custom validator shell command

How to enable:

```
item-validation {
    file {
        use-custom-validator = true
        custom-validator-command = "gzip -t ${fileName}"
    }
}
```

Notes:

`${fileName}` will be replaced with file item name

## Clean up policy

You can specify clean up policy per profile.

### Remove all invalid items

If you want to remove all invalid items you should enable property `all-invalid-items` in cleanup section:

```
someProfile {
    cleanup {      
        all-invalid-items = true
    }
}
```

### Do not remove invalid items in keep range

Sometimes invalid items may be incomplete but valuable. If you want to keep them with valid items you should
enable special property:

```
someProfile {
    cleanup {
        invalid-items-beyond-of-keep-quantity = true
        all-invalid-items = false
    }
}
```

This property depends on `all-invalid-items`.

## RoadMap

- Detect previous file items by regexp mask

- Support special clean up policies for scheme grandfather-father-son
