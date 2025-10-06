<#macro renderSchemaType schemaType>
<#compress>
<#if schemaType.getItems()??>
  <#if schemaType.getItems().getType()??>
< ${schemaType.getItems().getType()} > array
  <#elseif schemaType.getItems().get$ref()?has_content>
< <<${schemaType.getItems().get$ref()?replace("#", "")?replace("/", "_")},${schemaType.getItems().get$ref()?remove_beginning("#/components/schemas/")}>> > array
  <#else>
< object > array
  </#if>
<#elseif schemaType.getEnum()?has_content>
enum (${schemaType.getEnum()?join(", ")})
<#else>
  <#if schemaType.getType()??>
${schemaType.getType()}
  <#else>
    <#if schemaType.get$ref()??>
<<${schemaType.get$ref()?replace("#", "")?replace("/", "_")},${schemaType.get$ref()?remove_beginning("#/components/schemas/")}>>
    <#else>
      <#list schemaType.getAllOf() as allOfSchema>
<@renderSchemaType schemaType=allOfSchema />
      </#list>
    </#if>
  </#if>
  <#if schemaType.getFormat()?has_content>
(${schemaType.getFormat()})
  </#if>
</#if>
</#compress>
</#macro>