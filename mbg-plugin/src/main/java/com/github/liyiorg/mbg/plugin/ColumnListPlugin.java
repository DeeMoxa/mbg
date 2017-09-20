package com.github.liyiorg.mbg.plugin;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.internal.util.StringUtility;

import com.github.liyiorg.mbg.util.TopLevelClassUtil;

/**
 * ColumnListPlugin 适用于<br>
 *  
 * Mapper.selectByExample(Example) <br>
 * Mapper.selectByExampleWithBLOBs(Example)
 * @author LiYi
 *
 */
public class ColumnListPlugin extends PluginAdapter {
	
	private static final String TYPE_NAME = "_T_Y_P_E_";
	
	private final static String REMARKS_PROPERTY_NAME = "remarks";
	
	private static String DEFAULT_REMARKS;

	private String remarks;
	

	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		DEFAULT_REMARKS = properties.getProperty(REMARKS_PROPERTY_NAME, "0");
		String remarks_pro = introspectedTable.getTableConfiguration().getProperty(REMARKS_PROPERTY_NAME);
		if (StringUtility.stringHasValue(remarks_pro)) {
			remarks = remarks_pro;
		} else {
			remarks = DEFAULT_REMARKS;
		}
		
	}
	
	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		//获取 key column type 1
		Set<String> primaryKeyColumns = new LinkedHashSet<String>();
		for(IntrospectedColumn introspectedColumn : introspectedTable.getPrimaryKeyColumns()){
			primaryKeyColumns.add(introspectedColumn.getActualColumnName());
		}
		//获取 base column type 2
		Set<String> baseColumns = new LinkedHashSet<String>();
		for(IntrospectedColumn introspectedColumn : introspectedTable.getBaseColumns()){
			baseColumns.add(introspectedColumn.getActualColumnName());
		}
		//获取 blobs column type 3
		Set<String> bLOBColumns = new LinkedHashSet<String>();
		for(IntrospectedColumn introspectedColumn : introspectedTable.getBLOBColumns()){
			bLOBColumns.add(introspectedColumn.getActualColumnName());
		}
		
		//添加import
		topLevelClass.addImportedType(Set.class.getName());
		topLevelClass.addImportedType(LinkedHashSet.class.getName());
		topLevelClass.addImportedType("com.github.liyiorg.mbg.util.StringUtils");
		
		//添加静态变量 defaultBaseColumns
		TopLevelClassUtil.addField(context.getCommentGenerator(),topLevelClass, introspectedTable, true,true,new FullyQualifiedJavaType("Set<C>"),"defaultBaseColumns","C.group(1,2)",false);
		//添加成员变量 Base_Column_List
		TopLevelClassUtil.addField(context.getCommentGenerator(),topLevelClass, introspectedTable, false,false,new FullyQualifiedJavaType("String"),"Base_Column_List",null,true);
		
		if(bLOBColumns.size()>0){
			//添加静态变量 defaultBLOBColumns
			TopLevelClassUtil.addField(context.getCommentGenerator(),topLevelClass, introspectedTable, true,true,new FullyQualifiedJavaType("Set<C>"),"defaultBLOBColumns","C.group(3)",false);
			//添加成员变量 Blob_Column_List
			TopLevelClassUtil.addField(context.getCommentGenerator(),topLevelClass, introspectedTable, false,false,new FullyQualifiedJavaType("String"),"Blob_Column_List",null,true);
		}
		
		
		addMethod_includeColumn(topLevelClass, introspectedTable,bLOBColumns.size()>0);
		addMethod_excludeColumn(topLevelClass, introspectedTable,bLOBColumns.size()>0);
		
		//添加内部枚举C
		addInnerEnum_C(topLevelClass, introspectedTable, primaryKeyColumns, baseColumns, bLOBColumns);
		return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
	}

	/**
	 * 添加列枚举
	 * @param topLevelClass topLevelClass
	 * @param introspectedTable introspectedTable
	 * @param primaryKeyColumns primaryKeyColumns
	 * @param baseColumns baseColumns
	 * @param bLOBColumns bLOBColumns
	 */
	private void addInnerEnum_C(TopLevelClass topLevelClass, IntrospectedTable introspectedTable,
			Set<String> primaryKeyColumns, Set<String> baseColumns, Set<String> bLOBColumns) {
		InnerEnum innerEnum_C = new InnerEnum(new FullyQualifiedJavaType("C"));
		innerEnum_C.setVisibility(JavaVisibility.PUBLIC);
		innerEnum_C.setStatic(true);
		
		//添加构造项
		Map<String, String> constantMap = new LinkedHashMap<String, String>();
		buildEnumConstant(constantMap, introspectedTable.getPrimaryKeyColumns(), innerEnum_C, 1);
		buildEnumConstant(constantMap, introspectedTable.getBaseColumns(), innerEnum_C, 2);
		buildEnumConstant(constantMap, introspectedTable.getBLOBColumns(), innerEnum_C, 3);
		
		StringBuilder stringBuilder = new StringBuilder();
		// 添加表注释
		if(!"0".equals(remarks)){
			stringBuilder.append("//--------------------------------------------------")
			.append(System.lineSeparator())
			.append("\t\t//[").append(introspectedTable.getTableType()).append("]");
			if (StringUtility.stringHasValue(introspectedTable.getRemarks())) {
				stringBuilder.append("\t").append(introspectedTable.getRemarks());
			}
			stringBuilder.append(System.lineSeparator())
			.append("\t\t//--------------------------------------------------");
		}
		
		switch (remarks) {
		case "0":
			buildRemarksType0(innerEnum_C, stringBuilder, constantMap);
			break;
		default:
			buildRemarksType1(innerEnum_C, stringBuilder, constantMap);
		}
		
		Field field_type = new Field(TYPE_NAME, new FullyQualifiedJavaType("int"));
		field_type.setVisibility(JavaVisibility.PRIVATE);
		field_type.setFinal(true);
		innerEnum_C.addField(field_type);

		Method method_C = new Method("C");
		method_C.setVisibility(JavaVisibility.PRIVATE);
		method_C.setConstructor(true);
		method_C.addParameter(new Parameter(new FullyQualifiedJavaType("int"), "type"));
		method_C.addBodyLine("this." + TYPE_NAME + " = type;");
		innerEnum_C.addMethod(method_C);

		Method method_getValue = new Method("getValue");
		method_getValue.setVisibility(JavaVisibility.PUBLIC);
		method_getValue.setReturnType(new FullyQualifiedJavaType("int"));
		method_getValue.addBodyLine("return " + TYPE_NAME + ";");
		innerEnum_C.addMethod(method_getValue);
		
		
		Method method_group = new Method("group");
		method_group.setVisibility(JavaVisibility.PUBLIC);
		method_group.setStatic(true);
		method_group.addParameter(new Parameter(new FullyQualifiedJavaType("int..."), "types"));
		method_group.setReturnType(new FullyQualifiedJavaType("Set<C>"));
		method_group.addBodyLine("Set<C> set = null;");
		method_group.addBodyLine("for(int type : types){");
			method_group.addBodyLine("for(C c : C.values()){");
				method_group.addBodyLine("if(c.getValue() == type){");
					method_group.addBodyLine("if(set == null){");
						method_group.addBodyLine("set = new LinkedHashSet<C>();");
					method_group.addBodyLine("}");
					method_group.addBodyLine("set.add(c);");
				method_group.addBodyLine("}");
			method_group.addBodyLine("}");
		method_group.addBodyLine("}");
		method_group.addBodyLine("return set;");
		innerEnum_C.addMethod(method_group);
		
		context.getCommentGenerator().addEnumComment(innerEnum_C, introspectedTable);
		topLevelClass.addInnerEnum(innerEnum_C);
	}
	
	private void addMethod_includeColumn(TopLevelClass topLevelClass,IntrospectedTable introspectedTable,boolean blob) {
		Method method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setName("includeColumn");
		method.addParameter(new Parameter(new FullyQualifiedJavaType("C..."),"cs"));
		method.addBodyLine("Set<C> baseSet = null;");
		method.addBodyLine("Base_Column_List = null;");
		if(blob){
		method.addBodyLine("Set<C> blobSet = null;");
		method.addBodyLine("Blob_Column_List = null;");
		}
		method.addBodyLine("if (cs != null) {");
			method.addBodyLine("for (C c : cs) {");
				method.addBodyLine("switch (c.getValue()) {");
				method.addBodyLine("case 1: ");
				method.addBodyLine("case 2:");
					method.addBodyLine("if(baseSet == null){");
						method.addBodyLine("baseSet = new LinkedHashSet<C>();");
					method.addBodyLine("}");
					method.addBodyLine("baseSet.add(c);");
					method.addBodyLine("break;");
				if(blob){
				method.addBodyLine("case 3:");
					method.addBodyLine("if(blobSet == null){");
						method.addBodyLine("blobSet = new LinkedHashSet<C>();");
					method.addBodyLine("}");
					method.addBodyLine("blobSet.add(c);");
					method.addBodyLine("break;");	
				}
				method.addBodyLine("}");
			method.addBodyLine("}");
			method.addBodyLine("if(baseSet != null){");
				method.addBodyLine("Base_Column_List = StringUtils.collectionToDelimitedString(baseSet, \",\");");
			method.addBodyLine("}");
			if(blob){
			method.addBodyLine("if(blobSet != null){");
				method.addBodyLine("Blob_Column_List = StringUtils.collectionToDelimitedString(blobSet, \",\");");
			method.addBodyLine("}");
			}
		method.addBodyLine("}");
		
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);
	}
	
	private void addMethod_excludeColumn(TopLevelClass topLevelClass,IntrospectedTable introspectedTable,boolean blob) {
		Method method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setName("excludeColumn");
		method.addParameter(new Parameter(new FullyQualifiedJavaType("C..."),"cs"));
		method.addBodyLine("Set<C> baseSet = new LinkedHashSet<C>();");
		method.addBodyLine("baseSet.addAll(defaultBaseColumns);");
		method.addBodyLine("Base_Column_List = null;");
		if(blob){
		method.addBodyLine("Set<C> blobSet = new LinkedHashSet<C>();");
		method.addBodyLine("blobSet.addAll(defaultBLOBColumns);");
		method.addBodyLine("Blob_Column_List = null;");
		}
		method.addBodyLine("if (cs != null) {");
			method.addBodyLine("for (C c : cs) {");
				method.addBodyLine("switch (c.getValue()) {");
				method.addBodyLine("case 1: ");
				method.addBodyLine("case 2:");
					method.addBodyLine("baseSet.remove(c);");
					method.addBodyLine("break;");
				if(blob){
				method.addBodyLine("case 3:");
					method.addBodyLine("blobSet.remove(c);");
					method.addBodyLine("break;");
				}
				method.addBodyLine("}");
			method.addBodyLine("}");
			method.addBodyLine("Base_Column_List = StringUtils.collectionToDelimitedString(baseSet, \",\");");
			if(blob){
			method.addBodyLine("Blob_Column_List = StringUtils.collectionToDelimitedString(blobSet, \",\");");
			}
		method.addBodyLine("}");
		
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);
	}
	
	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
			builderXML(element);
		return super.sqlMapSelectByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
	}

	@Override
	public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		builderXML(element);
		return super.sqlMapSelectByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
	}

	/**
	 * 替换XML
	 * @param element element
	 */
	private void builderXML(XmlElement element) {
		int base_Column_List_index = 0;
		int blob_Column_List_index = 0;
		List<Element> elements = element.getElements();
		for (int i = 0; i < elements.size(); i++) {
			Element e = element.getElements().get(i);
			if (e instanceof XmlElement) {
				XmlElement exml = (XmlElement) e;
				if ("include".equals(exml.getName())) {
					for (Attribute attribute : exml.getAttributes()) {
						if ("refid".equals(attribute.getName()) && "Base_Column_List".equals(attribute.getValue())) {
							base_Column_List_index = i;
							break;
						}
						if ("refid".equals(attribute.getName()) && "Blob_Column_List".equals(attribute.getValue())) {
							blob_Column_List_index = i;
							break;
						}
					}
				}
			}
		}

		if (base_Column_List_index != 0) {
			XmlElement when = new XmlElement("when");
			when.addAttribute(new Attribute("test", "Base_Column_List != null"));
			when.addElement(new TextElement("${Base_Column_List}"));
			XmlElement otherwise = new XmlElement("otherwise");
			otherwise.addElement(elements.get(base_Column_List_index));
			XmlElement chooseXMLElement = new XmlElement("choose");
			chooseXMLElement.addElement(when);
			chooseXMLElement.addElement(otherwise);

			// 替换<include refid="Base_Column_List" /> 为 <choose>
			elements.remove(base_Column_List_index);
			elements.add(base_Column_List_index, chooseXMLElement);
		}

		if (blob_Column_List_index != 0) {
			XmlElement when = new XmlElement("when");
			when.addAttribute(new Attribute("test", "Blob_Column_List != null"));
			when.addElement(new TextElement("${Blob_Column_List}"));
			XmlElement otherwise = new XmlElement("otherwise");
			otherwise.addElement(elements.get(blob_Column_List_index));
			XmlElement chooseXMLElement = new XmlElement("choose");
			chooseXMLElement.addElement(when);
			chooseXMLElement.addElement(otherwise);

			// 替换<include refid="Blob_Column_List" /> 为 <choose>
			elements.remove(blob_Column_List_index);
			elements.add(blob_Column_List_index, chooseXMLElement);
		}
	}
	
	private void buildRemarksType0(InnerEnum innerEnum_C, StringBuilder stringBuilder,
			Map<String, String> constantMap) {
		Iterator<String> iterator = constantMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			stringBuilder.append(System.lineSeparator()).append("\t\t");
			stringBuilder.append(key);
			if (iterator.hasNext()) {
				stringBuilder.append(",");
			} 
		}
		innerEnum_C.addEnumConstant(stringBuilder.toString());
	}
	
	private void buildRemarksType1(InnerEnum innerEnum_C, StringBuilder stringBuilder,
			Map<String, String> constantMap) {
		try {
			int maxTabs = maxTabs(constantMap.values().toArray(),7);
			if(maxTabs > 10){
				maxTabs = 10;
			}
			Iterator<String> iterator = constantMap.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String columnRemark = constantMap.get(key);
				boolean hasRemark = StringUtility.stringHasValue(columnRemark);
				String rm = "\t\t\t";
				if(hasRemark){
					stringBuilder.append(System.lineSeparator());
					rm = "/** " + columnRemark + " */";
				}
				stringBuilder.append("\t\t");
				String brm = "";
				if(hasRemark && rm.length() <= 40){
					brm = columnRemark;
				}else{
					rm += System.lineSeparator() + "\t\t\t";
				}
				stringBuilder.append(rm).append(buildTabs(brm, maxTabs,7)).append(key);
				if (iterator.hasNext()) {
					stringBuilder.append(",");
				} 
			}
			innerEnum_C.addEnumConstant(stringBuilder.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 生成构造项
	 * @param map map
	 * @param introspectedColumns introspectedColumns
	 * @param innerEnum_C innerEnum_C
	 * @param type [1,2,3]
	 */
	private void buildEnumConstant(Map<String,String> map,List<IntrospectedColumn> introspectedColumns, InnerEnum innerEnum_C, int type) {
		for (IntrospectedColumn column : introspectedColumns) {
			String columnRemark = null;
			if (StringUtility.stringHasValue(column.getRemarks())) {
				columnRemark = column.getRemarks();
			}
			
			if(StringUtility.stringHasValue(column.getDefaultValue())){
				if(columnRemark == null){
					columnRemark = "[DV=>" + column.getDefaultValue() + "]";
				}else{
					columnRemark += " [DV=>" + column.getDefaultValue() + "]";
				}
			}
			String key = String.format("%s(%d)", column.getActualColumnName(), type);
			map.put(key, columnRemark);
		}
	}

	public boolean validate(List<String> warnings) {
		return true;
	}

	
	/**
	 * 计算格式化输出注释的最大key tab length
	 * @param keys keys
	 * @return maxTabs
	 */
	private int maxTabs(Object[] keys, int a) {
		int maxLengthKey = 0;
		for (int i = 0; i < keys.length; i++) {
			Object key = keys[i];
			if (key != null) {
				String keystr = key.toString().replaceAll("[^\\x00-\\xff]", "  ");
				if (keystr.length() > maxLengthKey) {
					maxLengthKey = keystr.length();
				}
			}
		}
		return (maxLengthKey + a) / 4 + ((maxLengthKey + a) % 4 == 0 ? 0 : 1);
	}

	/**
	 * 计算格式化输出注释的预留空白
	 * @param key key
	 * @param maxTabs maxTabs
	 * @return \t ...
	 */
	private static String buildTabs(String key, int maxTabs,int a) {
		key = key.replaceAll("[^\\x00-\\xff]", "  ");
		int keyLength =  key.length();
		int r = (keyLength + a) / 4; 
		int l = maxTabs - r + 1;
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < l; i++) {
			stringBuilder.append("\t");
		}
		return stringBuilder.toString();
	}
	
	public static void main(String[] args) {
		String key = "省份名称a";
		String a = buildTabs(key, 3, 7);
		System.out.println(a);
	}

	

}
