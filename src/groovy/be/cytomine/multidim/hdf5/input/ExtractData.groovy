/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package be.cytomine.multidim.hdf5.input;

import ch.systemsx.cisd.base.mdarray.MDShortArray;

/**
 * Created by laurent on 20.12.16.
 */
public abstract class ExtractData {

    public abstract MDShortArray extract2DCube(int startX, int startY, int dim, int wid, int hei, MDShortArray base)
    public abstract MDShortArray extract2DCube(int startX, int startY, int wid, int hei, int depth)
    public abstract int getImageWidth();
    public abstract int getImageHeight();
    public abstract int getImageDepth();
    public abstract void getImage(int i)

}
