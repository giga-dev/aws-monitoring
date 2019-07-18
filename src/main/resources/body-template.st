<html>
    <body>
      <div><p>
          Summary:
          <font color=red>Running: $diff.runningSize$ &uarr;,</font>
          <font color=red>Added: $diff.addedSize$ &uarr;,</font>
          <font color=green>Removed: $diff.removedSize$ &darr;</font>
       </p></div>
       $if(diff.hasAdded)$
       <div><p>
       <span style='font-size:40px;max-height:60px;margin:0px'>&#128176;</span><span style='font-size:16px;'>New Instances</span>
       </p></div>
       <div class="styling">
        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
            <tr>
                <th>Since</th>
                <th>Machine Type</th>
                <th>Started By</th>
                <th>Name</th>
                <th>Instance Id</th>
                <th>Profile</th>
                <th>Region</th>
            </tr>
            $added:{
              <tr>
                <td>$it.since$</td>
                <td>$it.type$</td>
                <td>$it.startedBy$</td>
                <td>$it.nameOr$</td>
                <td>$it.instanceId$</td>
                <td>$it.profile$</td>
                <td>$it.region$</td>
              </tr>
            }$
         </table>
        </div>
        $endif$

       $if(diff.hasUnchanged)$
       <div><p>
       <span style='font-size:40px;max-height:60px;margin:0px'>&#128176;</span><span style='font-size:16px;'>Unchanged Instances</span>
       </p></div>
       <div class="styling">
        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
            <tr>
                <th>Since</th>
                <th>Machine Type</th>
                <th>Started By</th>
                <th>Name</th>
                <th>Instance Id</th>
                <th>Profile</th>
                <th>Region</th>
            </tr>
            $unchanged:{
              <tr>
                <td>$it.since$</td>
                <td>$it.type$</td>
                <td>$it.startedBy$</td>
                <td>$it.nameOr$</td>
                <td>$it.instanceId$</td>
                <td>$it.profile$</td>
                <td>$it.region$</td>
              </tr>
            }$
         </table>
        </div>
        $endif$

       $if(diff.hasRemoved)$
       <div><p>
       <span style='font-size:40px;max-height:60px;margin:0px'>&#128077;</span><span style='font-size:16px;'>Removed Instances</span>
       </p></div>
       <div class="styling">
        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
            <tr>
                <th>Since</th>
                <th>Machine Type</th>
                <th>Started By</th>
                <th>Name</th>
                <th>Instance Id</th>
                <th>Profile</th>
                <th>Region</th>
            </tr>
            $removed:{
              <tr>
                <td>$it.since$</td>
                <td>$it.type$</td>
                <td>$it.startedBy$</td>
                <td>$it.nameOr$</td>
                <td>$it.instanceId$</td>
                <td>$it.profile$</td>
                <td>$it.region$</td>
              </tr>
            }$
         </table>
        </div>
        $endif$

     </body>
</html>