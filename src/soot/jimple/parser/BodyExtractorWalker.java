/* Soot - a J*va Optimization Framework
 * Copyright (C) 2000 Patrice Pominville
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package soot.jimple.parser;

import soot.baf.*;
import soot.*;
import soot.jimple.*;

import soot.jimple.parser.parser.*;
import soot.jimple.parser.lexer.*;
import soot.jimple.parser.node.*;
import soot.jimple.parser.analysis.*;

import java.io.*;
import java.util.*;


/**
 *  Walks a jimple AST and constructs the method bodies for all the methods of 
 *  the SootClass associated with this walker (see constructor). 
 *  note: Contrary to the plain "Walker", this walker does not create a SootClass,
 *  or interact with the scene. It merely adds method bodies for each of the methods of
 *  the SootClass it was initialized with.
 */
   
public class BodyExtractorWalker extends Walker
{
    Map methodToParsedBodyMap;

    /** Constructs a walker, and attaches it to the given SootClass, sending bodies to
     * the given methodToParsedBodyMap. */
    public BodyExtractorWalker(SootClass sc, SootResolver resolver, Map methodToParsedBodyMap) 
    {
        super(sc, resolver);
        this.methodToParsedBodyMap = methodToParsedBodyMap;
    }
    
    /*
      file = 
      modifier* file_type class_name extends_clause? implements_clause? file_body; 
    */       
    public void caseAFile(AFile node)
    {
        inAFile(node);
        {
            Object temp[] = node.getModifier().toArray();
            for(int i = 0; i < temp.length; i++)
            {
                ((PModifier) temp[i]).apply(this);
            }
        }
        if(node.getFileType() != null)
        {
            node.getFileType().apply(this);
        }
        if(node.getClassName() != null)
        {
            node.getClassName().apply(this);
        }
        
        String className = (String) mProductions.pop();
        if(!className.equals(mSootClass.getName()))
            throw new RuntimeException("expected:  " + className + ", but got: " + mSootClass.getName());

        if(node.getExtendsClause() != null)
        {
            node.getExtendsClause().apply(this);
        }
        if(node.getImplementsClause() != null)
        {
            node.getImplementsClause().apply(this);
        }
        if(node.getFileBody() != null)
        {
            node.getFileBody().apply(this);
        }
        outAFile(node);        
    }

    public void outAFile(AFile node)
    {        
        if(node.getImplementsClause() != null) 
            mProductions.pop(); // implements_clause
        
        if(node.getExtendsClause() != null) 
            mProductions.pop(); // extends_clause
        
        mProductions.pop(); // file_type
        
        mProductions.push(mSootClass);
    } 


    /*
      member =
      {field}  modifier* type name semicolon |
      {method} modifier* type name l_paren parameter_list? r_paren throws_clause? method_body;
    */    
    public void outAFieldMember(AFieldMember node)
    {
        mProductions.pop(); // name
        mProductions.pop(); // type
    }

    public void outAMethodMember(AMethodMember node)
    {
        int modifier = 0;
        Type type;
        String name;
        List parameterList = new ArrayList();
        List throwsClause = null;
        JimpleBody methodBody = null;

        if(node.getMethodBody() instanceof AFullMethodBody)
            methodBody = (JimpleBody) mProductions.pop();
        
        if(node.getThrowsClause() != null)
            throwsClause = (List) mProductions.pop();
        
        if(node.getParameterList() != null) {
            parameterList = (List) mProductions.pop();
        }

        name = (String) mProductions.pop(); // name
        type = (Type) mProductions.pop(); // type
        SootMethod sm = null;
        if (mSootClass.declaresMethod(SootMethod.getSubSignature(name, parameterList, type)))
        {
            sm = mSootClass.getMethod(SootMethod.getSubSignature(name, parameterList, type));
            if (soot.Main.isVerbose)
                System.out.println("[Jimple parser] " + SootMethod.getSubSignature(name, parameterList, type));
        }
        else
        {
            System.out.println("[!!! Couldn't parse !!] " + SootMethod.getSubSignature(name, parameterList, type));

	    
            System.out.println("[!] Methods in class are:");
            Iterator it = mSootClass.getMethods().iterator();
            while(it.hasNext()) {
                SootMethod next = (SootMethod) it.next();
                System.out.println(next.getSubSignature());
            }
            
        }

        if(sm.isConcrete()) 
        {
          if (soot.Main.isVerbose)
              System.out.println("[Parsed] "+sm.getDeclaration());

          methodBody.setMethod(sm);
          methodToParsedBodyMap.put(sm, methodBody);
        } 
        else if(node.getMethodBody() instanceof AFullMethodBody) {
            if(sm.isPhantom() && soot.Main.isVerbose)
               System.out.println("[jimple parser] phantom method!");
            throw new RuntimeException("Impossible: !concrete => ! instanceof " + sm.getName() );        
        }
    }
  
} 
