package io.micronaut.openapi.javadoc

import spock.lang.Specification

class JavadocParserSpec extends Specification {

    void 'test parse basic javadoc'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
This is a summary text. This is a description with <b>bold</b> and {@code some code}

@since 1.0
@param foo The foo param
@param bar The {@code bar} param
@return The {@value return} value
''')

        expect:
        desc.methodSummary
        desc.methodDescription
        desc.returnDescription
        desc.parameters.size() == 2
        desc.methodSummary == 'This is a summary text.'
        desc.methodDescription == 'This is a summary text. This is a description with **bold** and `some code`'
        desc.returnDescription == '''The

```
return
```

value'''
        desc.parameters['foo'] == 'The foo param'
        desc.parameters['bar'] == 'The `bar` param'

    }

    void 'test parse multiline javadoc'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
<p>This is a description with <b>bold</b> and {@code some code}.</p>

And more stuff.

@since 1.0
@param foo The foo param
@param bar The {@code bar} param
@return The {@value return} value
''')

        expect:
        desc.methodSummary
        desc.methodDescription
        desc.returnDescription
        desc.parameters.size() == 2
        desc.methodSummary == 'This is a description with **bold** and `some code`.'
        desc.methodDescription == '''This is a description with **bold** and `some code`.
And more stuff.'''
        desc.returnDescription == '''The

```
return
```

value'''
        desc.parameters['foo'] == 'The foo param'
        desc.parameters['bar'] == 'The `bar` param'
    }

    void 'test parse multiline return value javadoc'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
<p>This is a description with <b>bold</b> and {@code some code}.</p>

And more stuff.

@since 1.0
@param foo The foo param
With more foo param description
@param bar The {@code bar} param
@return The {@value return} value
with more return description

as it is multiline
''')

        expect:
        desc.methodSummary
        desc.methodDescription
        desc.returnDescription
        desc.parameters.size() == 2
        desc.parameters['foo'] == '''The foo param With more foo param description'''
        desc.parameters['bar'] == 'The `bar` param'
        desc.methodSummary == 'This is a description with **bold** and `some code`.'
        desc.methodDescription == '''This is a description with **bold** and `some code`.
And more stuff.'''
        desc.returnDescription == '''The

```
return
```

value with more return description as it is multiline'''
    }

    void 'test parse multiline javadoc param'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
Check if the given user has access to RabbitMQ.

@param username
                   The username to check.
@param password
                   The password to check.
@return 'allow' or 'deny' and user tags.

''')

        expect:
        desc.methodDescription
        desc.returnDescription
        desc.parameters.size() == 2
        desc.parameters['username'] == 'The username to check.'
        desc.parameters['password'] == 'The password to check.'
    }

    void 'test parse javadoc with summary tag'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
{@summary This is a summary text.} This is a description with <b>bold</b> and {@code some code}

@since 1.0
@param foo The foo param
@param bar The {@code bar} param
@return The {@value return} value
''')

        expect:
        desc.methodSummary
        desc.methodDescription
        desc.returnDescription
        desc.parameters.size() == 2
        desc.methodSummary == 'This is a summary text.'
        desc.methodDescription == 'This is a summary text. This is a description with **bold** and `some code`'
        desc.returnDescription == '''The

```
return
```

value'''
        desc.parameters['foo'] == 'The foo param'
        desc.parameters['bar'] == 'The `bar` param'

    }

    void 'test parse javadoc with complex javadoc'() {

        given:
        JavadocParser parser = new JavadocParser()
        JavadocDescription desc = parser.parse('''
Values separated with commas ",". In case of iterables, the values are converted to {@link String} and joined
with comma delimiter. In case of {@link Map} or a POJO {@link Object} the keys and values are alternating and all
delimited with commas.
<table border="1">
    <caption>Examples</caption>
    <tr> <th><b> Type </b></th>      <th><b> Example value </b></th>                     <th><b> Example representation </b></th> </tr>
    <tr> <td> Iterable &emsp; </td>   <td> param=["Mike", "Adam", "Kate"] </td>           <td> "param=Mike,Adam,Kate" </td></tr>
    <tr> <td> Map </td>              <td> param=["name": "Mike", "age": "30"] &emsp;</td> <td> "param=name,Mike,age,30" </td> </tr>
    <tr> <td> Object </td>           <td> param={name: "Mike", age: 30} </td>            <td> "param=name,Mike,age,30" </td> </tr>
</table>
Note that ambiguity may arise when the values contain commas themselves after being converted to String.
''')

        expect:
        desc.methodSummary
        desc.methodDescription
        desc.methodSummary == 'Values separated with commas ",".'
        desc.methodDescription == '''Values separated with commas ",". In case of iterables, the values are converted to String and joined with comma delimiter. In case of Map or a POJO Object the keys and values are alternating and all delimited with commas.

|  **Type**  |            **Example value**            | **Example representation** |
|------------|-----------------------------------------|----------------------------|
| Iterable   | param=\\["Mike", "Adam", "Kate"\\]        | "param=Mike,Adam,Kate"     |
| Map        | param=\\["name": "Mike", "age": "30"\\]   | "param=name,Mike,age,30"   |
| Object     | param={name: "Mike", age: 30}           | "param=name,Mike,age,30"   |
[Examples]

Note that ambiguity may arise when the values contain commas themselves after being converted to String.'''
    }
}
