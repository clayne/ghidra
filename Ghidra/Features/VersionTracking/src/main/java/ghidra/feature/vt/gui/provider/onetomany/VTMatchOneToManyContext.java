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
package ghidra.feature.vt.gui.provider.onetomany;

import java.util.ArrayList;
import java.util.List;

import docking.DefaultActionContext;
import ghidra.feature.vt.api.main.VTAssociation;
import ghidra.feature.vt.api.main.VTAssociationType;
import ghidra.feature.vt.api.main.VTMatch;

public class VTMatchOneToManyContext extends DefaultActionContext {

	private final List<VTMatch> selectedItems;

	VTMatchOneToManyContext(VTMatchOneToManyTableProvider provider, List<VTMatch> selectedItems) {
		super(provider, null);
		this.selectedItems = selectedItems;
	}

	public List<VTMatch> getSelectedMatches() {
		return selectedItems;
	}

	public int getSelectedRowCount() {
		return selectedItems.size();
	}

	public List<VTMatch> getFunctionMatches() {
		List<VTMatch> functionMatches = new ArrayList<>();

		for (VTMatch match : selectedItems) {
			VTAssociation association = match.getAssociation();
			if (association.getType() != VTAssociationType.FUNCTION) {
				continue;
			}

			functionMatches.add(match);
		}
		return functionMatches;
	}

}
