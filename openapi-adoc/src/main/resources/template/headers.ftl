<#if headers?has_content>

.Headers
[%header,caption=,cols=".^2a,.^14a,.^4a"]
!===
<.<!Name
<.<!Description
<.<!Schema
  <#list headers as headerName, h>

    <#if h.get$ref()?has_content>
        <#assign header = components.getHeaders()[h.get$ref()?remove_beginning("#/components/headers/")] />
    <#else>
        <#assign header = h />
    </#if>
<.<!${headerName}
    <#if header.getRequired()?? && header.getRequired()>
__required__
    </#if>
    <#if header.getDeprecated()?? && header.getDeprecated()>
__deprecated__
    </#if>
    <#if header.getExplode()?? && header.getExplode()>
__explode__
    </#if>
<.<!<#compress>
      <#if header.getDescription()?has_content>${header.getDescription()}</#if>
      <#if header.getExample()?has_content || header.getExamples()?has_content>
        <#if header.getExample()?has_content>
            <#assign example = header.getExample() />
        <#else>
            <#assign examples = header.getExamples() />
        </#if>
        <#include template_examples />
      </#if>
    </#compress>

<.<!<#assign schemaType = header.getSchema() /><#include template_schemaType />
  </#list>
!===
</#if>
