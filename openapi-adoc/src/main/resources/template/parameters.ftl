<#if parameters?has_content>
.Parameters
[%header,caption=,cols=".^2a,.^3a,.^10a,.^5a"]
|===
<.<|In
<.<|Name
<.<|Description
<.<|Type
  <#list parameters as p>

    <#if p.get$ref()?has_content>
      <#assign parameter = components.getParameters()[p.get$ref()?remove_beginning("#/components/parameters/")] />
    <#else>
      <#assign parameter = p />
    </#if>
<.<|**${parameter.getIn()}**
<.<|**${parameter.getName()}** +
    <#if parameter.getDeprecated()?? && parameter.getDeprecated()>
__deprecated__ +
    </#if>
    <#if parameter.getRequired()?? && parameter.getRequired()>
__required__ +
    </#if>
    <#if parameter.getAllowEmptyValue()?? && parameter.getAllowEmptyValue()>
__allow empty value__ +
    </#if>
    <#if parameter.getExplode()?? && parameter.getExplode()>
__explode__ +
    </#if>
    <#if parameter.getAllowReserved()?? && parameter.getAllowReserved()>
__allow reserved__ +
    </#if>
<.<|
<#if parameter.getDescription()?has_content>
${parameter.getDescription()}

    </#if>
    <#if parameter.getStyle()??>
Style: ${parameter.getStyle()} +
    </#if>
    <#if parameter.getExample()?has_content || parameter.getExamples()?has_content>
        <#if parameter.getExample()?has_content>
          <#assign example = parameter.getExample() />
        <#else>
          <#assign examples = parameter.getExamples() />
        </#if>
        <#include template_examples />
    </#if>
    <#if parameter.getContent()??>
      <#assign content = parameter.getContent() />
      <#include template_content />
    </#if>
<.<|<#if parameter.getSchema()??><#assign schemaType = parameter.getSchema() /><#include template_schemaType /></#if>
  </#list>
|===
</#if>
