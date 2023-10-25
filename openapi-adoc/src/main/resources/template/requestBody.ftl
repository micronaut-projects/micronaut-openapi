<#if requestBody??>
  <#if requestBody.get$ref()?has_content>
    <#assign request = components.getRequestBodies()[requestBody.get$ref()?remove_beginning("#/components/requestBodies/")] />
  <#else>
    <#assign request = requestBody />
  </#if>
==== Request
  <#if request.getDescription()?has_content>

${request.getDescription()}
  </#if>
  <#if request.getRequired()?? && request.getRequired()>

__required__
  </#if>
  <#if request.getContent()?has_content>

    <#assign content = request.getContent() />
    <#include template_content />
  </#if>
</#if>
