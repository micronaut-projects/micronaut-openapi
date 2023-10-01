<#if info??>
    <#if info.getTitle()?has_content>
= ${info.getTitle()}
    </#if>
    <#if info.getContact()?? && info.getContact().getName()?has_content>
${info.getContact().getName()}
    </#if>
    <#if info.getVersion()?has_content>
${info.getVersion()}
:revnumber: ${info.getVersion()}
    </#if>
    <#if info.getContact()??>
        <#if info.getContact().getEmail()?has_content>
:email: ${info.getContact().getEmail()}
        </#if>
        <#if info.getContact().getName()?has_content>
:author: ${info.getContact().getName()}
        </#if>
:authorcount: 1
    </#if>
</#if>
<#if openApi?has_content>
:openapi: ${openApi}
</#if>
<#if info??>

== Overview
    <#if info.getDescription()?has_content>

${info.getDescription()?trim}
    </#if>
    <#if info.getTermsOfService()?has_content>
        <#if info.getDescription()?has_content>

        </#if>
${info.getTermsOfService()}[Terms Of Service] +
    </#if>
    <#if info.getLicense()?has_content>
        <#if info.getDescription()?has_content && !info.getTermsOfService()?has_content>

        </#if>
${info.getLicense().getUrl()?has_content?then(info.getLicense().getUrl() + '[', '')}${info.getLicense().getName()}${info.getLicense().getUrl()?has_content?then(']', '')} +
    </#if>
    <#if externalDocs?has_content>
      <#if info.getDescription()?has_content && !info.getTermsOfService()?has_content && !info.getLicense()?has_content>

      </#if>
      <#include template_externalDocs />
    </#if>
</#if>
<#if tags?has_content>

== Tags

    <#list tags as tag>
${tag.getName()}::
        <#if tag.getDescription()?has_content>
${tag.getDescription()}
        </#if>
    </#list>
</#if>
<#include template_servers />
