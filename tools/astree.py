'''
Created on 2 Sep 2016

@author: u391812
'''
import ast
class ListTransformer(ast.NodeTransformer):
    def visit_Name(self,node):
        return ast.Str(node.id)  
    def visit_List(self,node):
        item_list=[]
        for item in node.elts:
            if type(item)==ast.Name:
                item_list.append(ast.Str(item.id))  
            else:
                item_list.append(item) 
        node.elts= item_list        
        return node
def node_filter(script='"BWARE" in [BWARE,COGAP1,DB2AP5,NZARP2,BIDRP10]'):
    tree=ast.parse(script,mode='eval')    
    print(ast.dump(tree))    
    ListTransformer().visit(tree)
    print (eval(compile(ast.fix_missing_locations(tree),'temp.py', mode='eval')))
    print(ast.dump(tree))
if __name__ == "__main__":
    node_filter()      
    import sys,getopt,os
    sys.path.append(sys.path[0])
    try:
        opts,args=getopt.getopt(sys.argv[1:],'hf:o:c:')
    except Exception as e:
        print ('input right parameters')