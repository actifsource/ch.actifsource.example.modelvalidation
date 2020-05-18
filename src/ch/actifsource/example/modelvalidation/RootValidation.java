package ch.actifsource.example.modelvalidation;

import java.util.List;
import ch.actifsource.core.CorePackage;
import ch.actifsource.core.INode;
import ch.actifsource.core.PackagedResource;
import ch.actifsource.core.Statement;
import ch.actifsource.core.job.IReadJobExecutor;
import ch.actifsource.core.job.Select;
import ch.actifsource.core.job.Update;
import ch.actifsource.core.model.aspects.IResourceValidationAspect;
import ch.actifsource.core.undo.GlobalEditContext;
import ch.actifsource.core.undo.IEditContext;
import ch.actifsource.core.update.IModifiable;
import ch.actifsource.core.util.LiteralUtil;
import ch.actifsource.core.validation.ValidationContext;
import ch.actifsource.core.validation.inconsistency.IResourceInconsistency;
import ch.actifsource.core.validation.inconsistency.SingleStatementInconsistency;
import ch.actifsource.core.validation.quickfix.AbstractQuickFix;
import ch.actifsource.core.validation.quickfix.IInconsistencyEnablement;


public class RootValidation implements IResourceValidationAspect {

  public class FixSpaceQuickFix extends AbstractQuickFix {
    
    protected final PackagedResource    fRootResource;
    
    private final Statement             fStatement;
    
    public FixSpaceQuickFix(String title, String description, Statement statement, PackagedResource rootResource, IInconsistencyEnablement enablement) {
      super(title, description, enablement);
      fStatement = statement;
      fRootResource = rootResource;
    }

    @Override
    protected void doApply(IModifiable modifiable) {
      String newName = Select.simpleName(modifiable, fStatement.subject()).replaceAll(" ", "");
      Update.modify(modifiable, fStatement, LiteralUtil.create(newName));
    }

    @Override
    protected IEditContext getEditContext() {
      return GlobalEditContext.DEFAULT;
    }
  }
  
  @Override
  public void validate(ValidationContext context, List<IResourceInconsistency> inconsistencyList) {
    INode resourceToValidate = context.getResource();
    ch.actifsource.core.Package pkg = context.getPackage();
    IReadJobExecutor executor = context.getReadJobExecutor();
    
    Statement nameStatement = Select.statementForAttributeOrNull(executor, CorePackage.NamedResource_name, resourceToValidate);
    if (nameStatement != null && Select.simpleName(executor, resourceToValidate).contains(" ")) {
      PackagedResource rootResource = Select.rootResource(executor, pkg, nameStatement.subject());
      
      // Add quick fix
      FixSpaceQuickFix fixSpaceQuickFix = new FixSpaceQuickFix("Remove space"+Select.simpleName(executor, rootResource.getResource()), "TODO", nameStatement, rootResource, new IInconsistencyEnablement() {

        @Override
        public boolean isEnabled() {
          return true;
        }
      });
      
      inconsistencyList.add(new SingleStatementInconsistency(nameStatement, "Contains space!", fixSpaceQuickFix));
    }
  }
  
}
