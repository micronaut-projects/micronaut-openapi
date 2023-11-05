<#if examples?has_content>
*Examples:*
  <#list examples as exampleName, ex>

.${exampleName}
    <#if ex.get$ref()?has_content>
      <#assign example = components.getExamples()[ex.get$ref()?remove_beginning("#/components/examples/")] />
    <#else>
      <#assign example = ex />
    </#if>
    <#if example.getSummary()?has_content>

${example.getSummary()}
    </#if>
    <#if example.getDescription()?has_content>

${example.getDescription()}
    </#if>
    <#if example.getExternalValue()?has_content>

${example.getExternalValue()}[Link]
    </#if>
    <#if example.getValue()??>
[source]
----
${JSON.writeValueAsString(example.getValue())}
----
    </#if>
<#--    <#if example.get$ref()?has_content><<${example.get$ref()?replace("#", "")?replace("/", "_")},${example.get$ref()?remove_beginning("#/components/examples/")}>></#if>-->
  </#list>
<#elseif example??>
*Example:*
[source]
----
${example}
----
</#if>
