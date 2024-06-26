/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.symtable;

import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressIterator;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.util.datastruct.Accumulator;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

class SymbolTableModel extends AbstractSymbolTableModel {

	SymbolTableModel(PluginTool tool) {
		super(tool);
	}

	@Override
	protected void doLoad(Accumulator<SymbolRowObject> accumulator, TaskMonitor monitor)
			throws CancelledException {
		if (symbolTable == null) {
			return;
		}

		SymbolIterator it = symbolTable.getDefinedSymbols();

		monitor.initialize(getKeyCount());
		int value = 0;
		while (it.hasNext()) {
			monitor.setProgress(value++);
			monitor.checkCancelled();
			Symbol s = it.next();
			if (filter.accepts(s, getProgram())) {
				accumulator.add(new SymbolRowObject(s));
			}
		}
		if (filter.acceptsDefaultLabelSymbols()) {
			AddressIterator addrIt = refMgr.getReferenceDestinationIterator(
				getProgram().getAddressFactory().getAddressSet(), true);
			while (addrIt.hasNext()) {
				monitor.setProgress(value++);
				monitor.checkCancelled();
				Address a = addrIt.next();
				Symbol s = symbolTable.getPrimarySymbol(a);
				if (s.isDynamic() && filter.accepts(s, getProgram())) {
					accumulator.add(new SymbolRowObject(s));
				}
			}
		}
	}

}
