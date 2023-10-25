<#if propSchema.getDescription()?has_content>
${propSchema.getDescription()}

</#if>
<#if propSchema.getExclusiveMinimum()?? && propSchema.getExclusiveMinimum()>
  __exclusive minimum__ +
</#if>
<#if propSchema.getExclusiveMaximum()?? && propSchema.getExclusiveMaximum()>
  __exclusive maximum__ +
</#if>
<#if propSchema.getWriteOnly()?? && propSchema.getWriteOnly()>
  __write-only__ +
</#if>
<#if propSchema.getReadOnly()?? && propSchema.getReadOnly()>
  __read-only__ +
</#if>
<#if propSchema.getDeprecated()?? && propSchema.getDeprecated()>
  __deprecated__ +
</#if>
<#if propSchema.getUniqueItems()?? && propSchema.getUniqueItems()>
  __unique items__ +
</#if>
<#if propSchema.getNullable()?? && propSchema.getNullable()>
  __nullable__ +
</#if>
<#if propSchema.getTitle()?has_content>
  **Title**: ${propSchema.getTitle()} +
</#if>
<#if propSchema.getDefault()?has_content>
  **Default**: ${propSchema.getDefault()} +
</#if>
<#if propSchema.getPattern()?has_content>
  **Pattern**: ${propSchema.getPattern()} +
</#if>
<#if propSchema.getMinProperties()??>
  **Multiple Of**: ${propSchema.getMultipleOf()} +
</#if>
<#if propSchema.getMaximum()??>
  **Maximum**: ${propSchema.getMaximum()} +
</#if>
<#if propSchema.getMinimum()??>
  **Minimum**: ${propSchema.getMinimum()} +
</#if>
<#if propSchema.getMaxLength()??>
  **Max. length**: ${propSchema.getMaxLength()} +
</#if>
<#if propSchema.getMinLength()??>
  **Min. length**: ${propSchema.getMinLength()} +
</#if>
<#if propSchema.getMaxItems()??>
  **Max. items**: ${propSchema.getMaxItems()} +
</#if>
<#if propSchema.getMinItems()??>
  **Min. items**: ${propSchema.getMinItems()} +
</#if>
<#if propSchema.getMaxProperties()??>
  **Max. properties**: ${propSchema.getMaxProperties()} +
</#if>
<#if propSchema.getMinProperties()??>
  **Min. properties**: ${propSchema.getMinProperties()} +
</#if>
<#if propSchema.getExample()?has_content || propSchema.getExamples()?has_content>
  <#if propSchema.getExample()?has_content>
    <#assign example = propSchema.getExample() />
  <#else>
    <#assign examples = propSchema.getExamples() />
  </#if>
  <#include template_examples />
</#if>
<#if propSchema.getProperties()?has_content>
  <#assign schemaProperties = propSchema.getProperties() />
  <#include template_properties />
</#if>
