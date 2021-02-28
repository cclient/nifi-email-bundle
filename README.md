Nifi SendEmail Processor
================================

```orginal

flowfile0                 mail0(flowfile0)

flowfile1  PutMail(1:1)   mail1(flowfile1)

flowfile2                 mail2(flowfile2)

```

```this

flowfile0            

flowfile1  SendMail(n:1)  mail(flowfile0,flowfile1,flowfile2)

flowfile2           


```

### 需求

* nifi生态和自定义processor下通常有failure stream的处理,为了及时发现failure需统一发送通知邮件

* upstream 里会堆积多项flowfile,1:1发送邮件则通知过多，因此n:1 把多项flowfile合并在一个邮件里发送(只发送flowfile的attribute信息)

* 邮件内容为table,每列是一个attribute,attribule取并集，每行是一个flowfile的所有attribute

基于官方的[PutMail](https://github.com/apache/nifi/blob/a9db5a8cb7313005b4077b66ce10ef81d3055ee8/nifi-nar-bundles/nifi-standard-bundle/nifi-standard-processors/src/main/java/org/apache/nifi/processors/standard/PutEmail.java) 开发

可以组合多个官方processor实现该功能，例如在前置Processor完成拼接email msg内容，直接通过putMail发送邮件

通过简单定制实现也是为了熟悉nifi的开发/测试规范

邮件为html body如下

<table border="1">
    <thead align="center" valign="middle">
    <tr>
        <th>a0</th>
        <th>a1</th>
        <th>a2</th>
        <th>a3</th>
        <th>filename</th>
        <th>path</th>
        <th>uuid</th>
        <th>entryDate</th>
        <th>size</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>v0</td>
        <td>v1</td>
        <td>v2</td>
        <td>v3</td>
        <td>184775642434972.mockFlowFile</td>
        <td>target</td>
        <td>1bdaef1f-960b-48c5-a739-a881f92d1466</td>
        <td>Thu May 21 15:37:00 CST 2020</td>
        <td></td>
    </tr>
    <tr>
        <td>v0</td>
        <td>v1</td>
        <td>v2</td>
        <td>v3</td>
        <td>184323287499017.mockFlowFile</td>
        <td>target</td>
        <td>45caab10-50c4-4727-ac42-bc345670cf85</td>
        <td>Thu May 21 15:37:00 CST 2020</td>
        <td>Thu Jan 01 08:00:00 CST 1970</td>
    </tr>    
    </tbody>
</table>


### deploy

#### 1 compile

`mvn package -Dmaven.test.skip=true`

#### 2 upload to one of

```nifi

nifi.nar.library.directory=./lib
nifi.nar.library.directory.custom=./lib_custom
nifi.nar.library.autoload.directory=./extensions
nifi.nar.working.directory=./work/nar/

```

cp nifi-email-nar/target/nifi-email-nar-0.1.nar nifi/lib_custom/

#### 3 restart nifi if need

nifi/bin/nifi.sh restart
