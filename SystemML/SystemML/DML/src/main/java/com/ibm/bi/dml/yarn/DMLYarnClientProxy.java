/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */

package com.ibm.bi.dml.yarn;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.bi.dml.api.DMLException;
import com.ibm.bi.dml.conf.DMLConfig;
import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.controlprogram.Program;
import com.ibm.bi.dml.runtime.controlprogram.ProgramBlock;
import com.ibm.bi.dml.yarn.ropt.ResourceConfig;
import com.ibm.bi.dml.yarn.ropt.ResourceOptimizer;
import com.ibm.bi.dml.yarn.ropt.YarnClusterAnalyzer;
import com.ibm.bi.dml.yarn.ropt.YarnClusterConfig;
import com.ibm.bi.dml.yarn.ropt.YarnOptimizerUtils.GridEnumType;

/**
 * The sole purpose of this class is to serve as a proxy to
 * DMLYarnClient to handle class not found exceptions or any
 * other issues of spawning the DML App Master.
 * 
 */
public class DMLYarnClientProxy 
{	
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2014\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	private static final Log LOG = LogFactory.getLog(DMLYarnClientProxy.class);

	protected static final boolean RESOURCE_OPTIMIZER = false;
	protected static final boolean LDEBUG = false;
	
	static
	{
		// for internal debugging only
		if( LDEBUG ) {
			Logger.getLogger("com.ibm.bi.dml.yarn").setLevel((Level) Level.DEBUG);
		}
	}
	
	/**
	 * 
	 * @param dmlScriptStr
	 * @param conf
	 * @param allArgs
	 * @return
	 * @throws IOException 
	 * @throws DMLRuntimeException 
	 */
	public static boolean launchDMLYarnAppmaster(String dmlScriptStr, DMLConfig conf, String[] allArgs, Program rtprog) 
		throws IOException, DMLRuntimeException
	{
		boolean ret = false;
		
		try
		{
			//optimize resources (and update configuration)
			if( RESOURCE_OPTIMIZER ){
				YarnClusterConfig cc = YarnClusterAnalyzer.getClusterConfig();
				ArrayList<ProgramBlock> pb = getRuntimeProgramBlocks(rtprog);
				ResourceConfig rc = ResourceOptimizer.optimizeResourceConfig( pb, cc, 
						              //    GridEnumType.EQUI_GRID, GridEnumType.EQUI_GRID );
						              GridEnumType.HYBRID2_MEM_EXP_GRID, GridEnumType.EQUI_GRID );
				conf.updateYarnMemorySettings(rc.getCPResource(), rc.getMaxMRResource());
			}
				
			//launch dml yarn app master
			DMLYarnClient yclient = new DMLYarnClient(dmlScriptStr, conf, allArgs);
			ret = yclient.launchDMLYarnAppmaster();
		}
		catch(NoClassDefFoundError ex)
		{
			LOG.warn("Failed to instantiate DML yarn client " +
					 "(NoClassDefFoundError: "+ex.getMessage()+"). " +
					 "Resume with default client processing.");
			ret = false;
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param args
	 * @return
	 * @throws DMLException
	 */
	private static ArrayList<ProgramBlock> getRuntimeProgramBlocks(Program prog) 
		throws DMLRuntimeException
	{			
		//construct single list of all program blocks including functions
		ArrayList<ProgramBlock> ret = new ArrayList<ProgramBlock>();
		ret.addAll(prog.getProgramBlocks());
		ret.addAll(prog.getFunctionProgramBlocks().values());
		
		return ret;
	}
}