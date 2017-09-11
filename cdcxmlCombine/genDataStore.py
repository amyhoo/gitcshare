'''
Created on 13 Dec 2016

@author: u391812 ,Xu Ya Min(Amy)
'''
try: 
  import xml.etree.cElementTree as ET  
except ImportError: 
  import xml.etree.ElementTree as ET  
import logging
import sys, getopt, os
import copy
import datetime

def findDescendent(node, tag):
    '''
    find first Descendent with the tag
    '''
    finder = node.iter(tag)
    elem = finder.next()
    return elem

def updateNode(tNode, sNode, attriKey, attriDict):
    '''
    using sNode information to update tNode, update the attribute in attriKey
    also if attriDict,then update tNode with this directly
    '''
    for key in attriKey:
        tNode.set(key, sNode.get(key))
    for key in attriDict:
        tNode.set(key, attriDict.get(key))
    return tNode

def copyEmptyElement(node):
    '''
    copy the Element without sub node
    '''    
    new_node = copy.deepcopy(node)    
    for subNode in new_node.findall("*"):            
        new_node.remove(subNode)
    return new_node    

def copyElement(node):
    '''
    copy the Element without sub node
    '''    
    new_node = copy.deepcopy(node)    
    return new_node    

def checkNode(node, root=None, attribs={}):
    '''
    find the similar node in the descendent of root,
    if the attribs and tag of node are likewise
    if attribs is {}, then all attribs of node and tag of node shoulde be likewise            
    '''   
    
    tagName = node.tag        
    nodeAttribSet = set(node.items())        
    for subNode in root.iter(tagName):
        if attribs and (set(attribs.items()) < set(subNode.items())):  # check the attribs equal
            return True                    
        elif set(subNode.items()) == nodeAttribSet:
            return True            
    return False

class SubScript():
    def __init__(self, xmlPath):
        if ET.iselement(xmlPath):        
            self.tree = ET.ElementTree(element=xmlPath)
            self.root = self.tree.getroot()
        else:
            self.tree = ET.parse(xmlPath)
            self.root = self.tree.getroot()            
    def findDescendent(self, tag):
        finder = self.root.iter(tag)
        elem = finder.next()
        return elem
                
    def findDescendents(self, tag):
        finder = self.root.iter(tag)
        return finder
    
    def copyClearTree(self):        
        new_root = copyEmptyElement(self.root)    
        new_root.set("timestamp", datetime.datetime.today().strftime("%Y-%m-%d-%H:%M:%S"))
        new_tree = SubScript(new_root)        
        return new_tree

    def getCDC_SOURCE_SYSTEM_CODE(self):
        for node in self.findDescendents('ColumnMapping'):
            if node.get('targetColumnName') == 'CDC_SOURCE_SYSTEM_CODE':
                return node.get('defaultValue')
        print ("the source xml can't find attribute CDC_SOURCE_SYSTEM_CODE in ColumnMapping node")

    def checkNode(self, node, root=None, attribs={}):
        '''
        check node if the node exist in this xml,the tab name, the attributes of this node
        if xmlPath then find the subtree of this xmlPath, check the node in the subtree        
        '''   
        if root:
             subtree = root
        else:
            subtree = self.root
        return checkNode(node, subtree, attribs)
    
    def getUserExitFolder(self):
        image_folder = self.findDescendent("UserExit").get("userExitUpdateTableImage")        
        return image_folder
    
class FlatMapping():
    modelScript = modelScript = SubScript("model.xml")
    def __init__(self, sources, desXmlPath, userRoot="/datawarehouse/development/dstage_nz/data/cdc_output/"):
        try:
            index = desXmlPath.rindex('/')
        except:
            index = 0
        self.subScriptName = desXmlPath[index:].split('.')[0]        
        self.source_list = sources.split(",")
        self.destXmlPath = desXmlPath
        self.userRoot = userRoot              
        self.destXml = self.modelScript.copyClearTree() 
        self.tree = self.destXml.tree   
        self.root = self.destXml.root         
              
    def checkTableMapping(self, tableMapping,attribs={}):
        '''
        check if this table mapping exist already in the script
        '''
        return self.destXml.checkNode(tableMapping,root=None,attribs=attribs)
        
    def checkColumnMapping(self, columnMapping, root, attribs={}):
        '''
        check if this columnMapping exist already in the script
        '''
        return self.destXml.checkNode(columnMapping,root,attribs)
    def getModelColumns(self):
        columnName_list = []
        for SourceColumn in self.modelScript.findDescendents("SourceColumn"):
            columnName = SourceColumn.get('columnName')
            columnName_list.append(columnName)
        return columnName_list
    def addTableMapping(self, sourceXml,needCheckOn):
        '''
        sourceXml:the sourceXml
        needCheckOn: if the sourceXml is the first of the list, then don't need check 
        add TableMapping node of source to subScriptNode
        '''
        sourceScript = SubScript(sourceXml)
        for TableMapping  in sourceScript.findDescendents("TableMapping"):
            new_TableMapping = copyEmptyElement(self.modelScript.findDescendent("TableMapping"))
            new_TableMapping = updateNode(new_TableMapping, TableMapping, ['publishedTableName', 'publishedUser', 'sourceTableName', 'sourceUser'], {})
            
            sourceTableName=new_TableMapping.get('sourceTableName')
            if needCheckOn and self.checkTableMapping(new_TableMapping,{'sourceTableName':sourceTableName}):
                
                # if the table already exists ,then skip this table.
                continue
            self.subScriptNode.append(new_TableMapping)
            for SourceColumn in TableMapping.iter("SourceColumn"):    
                new_TableMapping.append(SourceColumn)            

            #if the source xml lack model SourceColumn ,will add it             
            for SourceColumn in self.modelScript.findDescendents("SourceColumn"):                
                # check if the column exist already in new_TableMapping
                columnName = SourceColumn.get('columnName')
                if self.checkColumnMapping(SourceColumn, new_TableMapping, {"columnName":columnName}):
                    continue
                if SourceColumn.get('columnName') == 'CDC_SOURCE_SYSTEM_CODE':
                    system_code = sourceScript.getCDC_SOURCE_SYSTEM_CODE() 
                    if system_code:
                       SourceColumn = updateNode(SourceColumn, None, [], {'derivedExpression':"'" + system_code + "'"})
                    new_TableMapping.append(SourceColumn)
            new_TargetColumn = copyEmptyElement(self.modelScript.findDescendent("TargetColumn"))
            new_TableMapping.append(new_TargetColumn)
            new_ColumnMapping = copyEmptyElement(self.modelScript.findDescendent("ColumnMapping"))
            new_TableMapping.append(new_ColumnMapping)
            new_UserExit = copyElement(self.modelScript.findDescendent("UserExit"))
            new_UserExit = updateNode(new_UserExit, None, [], {'userExitUpdateTableImage':self.modelScript.getUserExitFolder() + self.userRoot + self.subScriptName})
            new_TableMapping.append(new_UserExit)
    @property    
    def subScriptNode(self):
        return self.root.find("Subscription")
    
    def genStoreXml(self):
        '''
        '''           
        self.root.append(copyEmptyElement(self.modelScript.findDescendent("Subscription")))  
        print(ET.tostring(self.root))        
        for index,source in enumerate(self.source_list):            
            self.addTableMapping(source,index>0)
        self.subScriptNode.append(self.modelScript.findDescendent("Notification"))
        print(ET.tostring(self.root))
        self.tree.write(self.destXmlPath, encoding='UTF-8')
    
if __name__ == "__main__":    
    sys.path.append(sys.path[0])
    try:
        # s represent source,d represent destination, r represent User-root
        opts, args = getopt.getopt(sys.argv[1:], 'hs:d:r:', ['help', 'source=', 'dest=', 'root='])
    except Exception as e:
        print ('input right parameters')   
    user_root = ''
    for op, value in opts:
        if op in ('-h', '--help'):
            print("""
            this program is to build a flat file mapping Subscription xml from the original Netezza mapping Subscription xml
            -h/--help:show help message
            -s/--source:Netezza mapping xmls of CDC mapping,each xml file will be seperate by ,
            -d/--dest:flat file mapping Subscription xml generated,the file name should be the format of "Subscription_name.xml"
            -r/--root:the user root of output files that generate by the CDC flat file mapping,example "/datawarehouse/development/dstage_nz/data/cdc_output/" 
            """)            
        elif op in ('-s', '--source'):
            source_xml = value
        elif op in ('-d', '--dest'):
            dest_xml = value
        elif op in ('-r', '--root'):
            user_root = value
    if user_root:
        gen = FlatMapping(source_xml, dest_xml, user_root)
    else:
        gen = FlatMapping(source_xml, dest_xml)
    gen.genStoreXml()
