// Импорт необходимых библиотек
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme
import com.atlassian.jira.component.ComponentAccessor
 
// Получение менеджера пользовательских полей и менеджера экранов
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def fieldScreenManager = ComponentAccessor.getFieldScreenManager()
 
// id полей, которые нас интересуют
def fieldNameList = [
    10001,
    10002,
    10003 
     
    ]
 
fieldNameList = fieldNameList.collect { "customfield_${it}" }
 
// Получение всех экранов в Jira
def fieldScreens = fieldScreenManager.getFieldScreens()
 
// Получение всех схем экранов для типов задач
def issueTypeScreenSchemeList = ComponentAccessor.getIssueTypeScreenSchemeManager().getIssueTypeScreenSchemes()
 
// Функция для получения ключей проектов по идентификаторам экранов
List<String> getProjectKeysByFieldScreenIds (Collection<IssueTypeScreenScheme> ITSS, List<Long> fieldScreenIds){
    ITSS.findAll { scheme -> // Возвращает те IssueTypeScreenScheme, в которых те Entities, которые ниже отфильтрованы
        // Проверка схем на наличие экранов с нужными полями
        scheme.entities.any { screenSchemeEntity -> // Возвращает те Entities, в которых те fieldScreenSchemeItems, которые ниже отфильтрованы
            // Проверка экранов в схеме на наличие нужных полей
            screenSchemeEntity.fieldScreenScheme.fieldScreenSchemeItems.any { screen -> //Возвращает те fieldScreenSchemeItems, которые проходят условие "содержатся в fieldScreenIds"
                fieldScreenIds.contains(screen.fieldScreenId)
            }
        }
    }.collect { scheme ->
        // Получение ключей проектов из найденных схем
        scheme.projects.collect { project ->
            project.key
        }
    }.flatten().unique() as List<String>// Удаление дубликатов и слияние списков ключей
}
 
def result = getProjectsKeysByFieldIds(issueTypeScreenSchemeList, fieldNameList as List<String>)
 
def getProjectsKeysByFieldIds (Collection<IssueTypeScreenScheme> ITSS, List<String> customfieldIds){
    def fieldScreenManager = ComponentAccessor.getFieldScreenManager()
    def fieldScreens = fieldScreenManager.getFieldScreens()
    def issueTypeScreenSchemeList = ComponentAccessor.getIssueTypeScreenSchemeManager().getIssueTypeScreenSchemes()
    def workflowManager = ComponentAccessor.getWorkflowManager()
    def workflows = workflowManager.getWorkflows()
    def projects = ComponentAccessor.getProjectManager().getProjectObjects()
    def wfsManager = ComponentAccessor.getWorkflowSchemeManager()
 
 
    def cf2proj = customfieldIds.collectEntries { customfieldId ->
    def foundScreens = fieldScreens.findAll { it.containsField(customfieldId) }   
 
    List<String> foundProjectsByScreens = getProjectKeysByFieldScreenIds(issueTypeScreenSchemeList, foundScreens*.id) as List<String>
 
    def foundwf = workflows.findAll { wf ->
        foundScreens.findAll { fs ->
            fs.containsField(customfieldId) && wf.getActionsForScreen(fs).size() > 0
        }  
    }
     
    def foundProj = projects.findAll { project ->
        def wfmap = wfsManager.getWorkflowMap(project)
        def wfInProj = wfmap.collect { key, value ->
            workflowManager.getWorkflow(value)
        }
        wfInProj.intersect(foundwf).size() > 0
    }
 
    foundProjectsByScreens.addAll(foundProj*.getKey())
    return [customfieldId, foundProjectsByScreens.unique()]
}
return cf2proj
}
 
 
 
// Вывод результатов
result.collect {key, value ->
    "${key} : ${value} ;\n"
}
